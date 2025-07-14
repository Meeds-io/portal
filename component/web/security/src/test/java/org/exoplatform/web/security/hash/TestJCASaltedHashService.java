/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.web.security.hash;

import org.exoplatform.component.test.*;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/secure-random-service-configuration.xml")})
public class TestJCASaltedHashService extends AbstractKernelTest {

    private static final String[] PASSWORDS = new String[] { "qwertzui", "123 asas", "\t\ndsdsds",
            "\u013E\u0161\u010D\u0165\u017E\u00FD\u00E1\u00ED\u00E9\u00FA\u00E4\u00F4", "2YMj5qZ3", "8BtRheTb", "jGHQJtzy",
            "qZgvgyg4", "xm92RQPM", "deRHPSZb", "xanKbLEG", "6fQTVvWg", "JCYgrj4s", "8j77Tq3f", "YyxNvqRq", "YDhGgEXZ",
            "bkS7gV8T", "X6e6d69F", "yFytCE7h", "8ABSkRnE", "P4AE8rEj", "ZRpncQBz", "JradAPdb", "Bz9NuXTM", "MNpeHysj",
            "zgh7pUak", "HpjPT6ve", "fhBBMPDp", "xkTepczM", "2zTjchKX", "C2YZeP8f", "MbYqrWbe", "3V9CSJxB", "9Kg4WfXq",
            "NS7CcCJg", "kTtHscqP", "YhCTFgBf", "zawBwenh", "8d2JRE8m", "nzryr4ZB", "QpAt9WSm", "nyA97jey", "Ge25puWQ",
            "MwFgvSAM", "K9GfwZNY", "ZH2nr24b", "ucRAa3zK", "7JqWx4yL", "QSGpjzE9", "PABCDz9U", "Mpn89JTw", "PqP3ShnV",
            "2zuNBEfY", "6wXbxBnL", "FcXTBkqp", "Yd3uJcXL", "kpxYUdsb", "ewEx8M5k", "Nu7mRktk", "Jk5qmyUf", "9z3tmdsq",
            "Rmn2DHme", "39rEjZHG", "C8pEpEBR", "c9AynEdd", "kWZknxTh", "A44XR2yP", "sUMd2YfZ", "zbZD8YJG", "e3pZUMGT",
            "SZ3jdTWf", "vFWM2BHf", "6faUceAF", "xTEzHV2b", "sUZdr5rk", "WDarLMzj", "kmkNzc72", "SnnCYV8g", "3KHd2dMc",
            "pzeHG9PT", "Zj3nXHY2", "n2gMR7QD", "NTabyr72", "kExtNfrH", "V8wvqUNr", "7xcUkGrD", "hvbEjyMy", "AzsS8ubZ",
            "TeqwLWMW", "wM4N8sSF", "9aU2unTB", "zdyJqZDX", "N7Q67gjE", "wCAqzZ6k", "WAvPAKY6", "kknz9DJu", "LjThRSGU",
            "pPwNSs56", "k7NXSwCC", "xUHNjFgn", "D8J8XWbK", "NfPBZLfU", "ZjpgMxEw", "63vtR97V" };

    public void test() throws InterruptedException, SaltedHashException {
        SaltedHashService sh = new JCASaltedHashService();
//        AutoReseedRandom random = new AutoReseedRandom(AutoReseedRandom.DEFAULT_RANDOM_ALGORITHM, AutoReseedRandom.DEFAULT_RANDOM_ALGORITHM_PROVIDER,
//                AutoReseedRandom.DEFAULT_SEED_LENGTH, 500);
        for (String password : PASSWORDS) {
            String saltedHash = sh.getSaltedHash(password);
            assertTrue(sh.validate(password, saltedHash));
        }
    }

}
