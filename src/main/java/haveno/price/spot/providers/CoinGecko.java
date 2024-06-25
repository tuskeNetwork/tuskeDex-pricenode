/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package haveno.price.spot.providers;

import haveno.price.spot.ExchangeRate;
import haveno.price.spot.ExchangeRateProvider;
import haveno.price.util.coingecko.CoinGeckoMarketData;

import haveno.price.util.coingecko.CoinGeckoTicker;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
class CoinGecko extends ExchangeRateProvider {
    private static final String GET_EXCHANGE_RATES_URL = "https://api.coingecko.com/api/v3/exchange_rates";

    public CoinGecko(Environment env) {
        super(env, "COINGECKO", "coingecko", Duration.ofMinutes(1));
    }

    @Override
    public Set<ExchangeRate> doGet() {

        // Rate limit for the CoinGecko API is 10 calls each second per IP address
        // We retrieve all rates in bulk, so we only make 1 call per provider poll

        Set<ExchangeRate> result = new HashSet<ExchangeRate>();

        Predicate<Map.Entry> isDesiredFiatPair = t -> getSupportedFiatCurrencies().contains(t.getKey());
        Predicate<Map.Entry> isDesiredCryptoPair = t -> getSupportedCryptoCurrencies().contains(t.getKey());

        Map<String, CoinGeckoTicker> rates = getMarketData().getRates();
        rates.entrySet().stream()
                .filter(isDesiredFiatPair.or(isDesiredCryptoPair))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                .forEach((key, ticker) -> {

                    boolean useInverseRate = getSupportedCryptoCurrencies().contains(key);
                    // Use inverse rate for alts, because the API returns the
                    // conversion rate in the opposite direction than what we need
                    // API returns the BTC/Alt rate, we need the Alt/BTC rate

                    // Find the inverse rate, while using enough decimals to reflect very
                    // small exchange rates
                    BigDecimal rate = ticker.getValue();
                    BigDecimal inverseRate = (rate.compareTo(BigDecimal.ZERO) > 0) ?
                            BigDecimal.ONE.divide(rate, 8, RoundingMode.HALF_UP) :
                            BigDecimal.ZERO;

                    result.add(new ExchangeRate(
                            useInverseRate ? key : "BTC",
                            useInverseRate ? "BTC" : key,
                            useInverseRate ? inverseRate : rate,
                            new Date(),
                            this.getName()
                    ));
                });
        return result;
    }

    private CoinGeckoMarketData getMarketData() {
        return WebClient.create().get()
                .uri(CoinGecko.GET_EXCHANGE_RATES_URL)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(CoinGeckoMarketData.class)
                .block(Duration.of(30, ChronoUnit.SECONDS));
    }
}
