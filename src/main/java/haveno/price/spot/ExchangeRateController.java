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

package haveno.price.spot;

import haveno.common.config.Config;
import haveno.price.PriceController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
class ExchangeRateController extends PriceController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping(path = "/getAllMarketPrices")
    public Map<String, Object> getAllMarketPrices() {
        return exchangeRateService.getAllMarketPrices();
    }

    static String translateFieldName(String name) {
        if (name.equals(Config.LEGACY_FEE_DATAMAP))
            name = Config.BTC_FEE_INFO;                 // name changed for clarity
        return name;
    }
}
