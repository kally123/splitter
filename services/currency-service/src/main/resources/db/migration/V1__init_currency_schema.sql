-- Currency Service Database Schema

-- Supported currencies table
CREATE TABLE IF NOT EXISTS currencies (
    code VARCHAR(3) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    decimal_places INTEGER DEFAULT 2,
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 999
);

-- Exchange rates table
CREATE TABLE IF NOT EXISTS exchange_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    base_currency VARCHAR(3) NOT NULL,
    target_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(19, 6) NOT NULL,
    rate_date DATE NOT NULL,
    provider VARCHAR(50) NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_base_currency FOREIGN KEY (base_currency) REFERENCES currencies(code),
    CONSTRAINT fk_target_currency FOREIGN KEY (target_currency) REFERENCES currencies(code)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_exchange_rates_lookup 
    ON exchange_rates(base_currency, target_currency, rate_date);
CREATE INDEX IF NOT EXISTS idx_exchange_rates_date 
    ON exchange_rates(rate_date);

-- Insert common currencies
INSERT INTO currencies (code, name, symbol, decimal_places, is_active, display_order) VALUES
    ('USD', 'US Dollar', '$', 2, true, 1),
    ('EUR', 'Euro', '€', 2, true, 2),
    ('GBP', 'British Pound', '£', 2, true, 3),
    ('JPY', 'Japanese Yen', '¥', 0, true, 4),
    ('CAD', 'Canadian Dollar', 'C$', 2, true, 5),
    ('AUD', 'Australian Dollar', 'A$', 2, true, 6),
    ('CHF', 'Swiss Franc', 'CHF', 2, true, 7),
    ('CNY', 'Chinese Yuan', '¥', 2, true, 8),
    ('INR', 'Indian Rupee', '₹', 2, true, 9),
    ('MXN', 'Mexican Peso', '$', 2, true, 10),
    ('BRL', 'Brazilian Real', 'R$', 2, true, 11),
    ('KRW', 'South Korean Won', '₩', 0, true, 12),
    ('SGD', 'Singapore Dollar', 'S$', 2, true, 13),
    ('HKD', 'Hong Kong Dollar', 'HK$', 2, true, 14),
    ('NOK', 'Norwegian Krone', 'kr', 2, true, 15),
    ('SEK', 'Swedish Krona', 'kr', 2, true, 16),
    ('DKK', 'Danish Krone', 'kr', 2, true, 17),
    ('NZD', 'New Zealand Dollar', 'NZ$', 2, true, 18),
    ('ZAR', 'South African Rand', 'R', 2, true, 19),
    ('RUB', 'Russian Ruble', '₽', 2, true, 20),
    ('TRY', 'Turkish Lira', '₺', 2, true, 21),
    ('PLN', 'Polish Zloty', 'zł', 2, true, 22),
    ('THB', 'Thai Baht', '฿', 2, true, 23),
    ('IDR', 'Indonesian Rupiah', 'Rp', 0, true, 24),
    ('PHP', 'Philippine Peso', '₱', 2, true, 25),
    ('CZK', 'Czech Koruna', 'Kč', 2, true, 26),
    ('ILS', 'Israeli Shekel', '₪', 2, true, 27),
    ('AED', 'UAE Dirham', 'د.إ', 2, true, 28),
    ('SAR', 'Saudi Riyal', '﷼', 2, true, 29),
    ('MYR', 'Malaysian Ringgit', 'RM', 2, true, 30),
    ('HUF', 'Hungarian Forint', 'Ft', 0, true, 31),
    ('CLP', 'Chilean Peso', '$', 0, true, 32),
    ('COP', 'Colombian Peso', '$', 0, true, 33),
    ('PEN', 'Peruvian Sol', 'S/', 2, true, 34),
    ('ARS', 'Argentine Peso', '$', 2, true, 35),
    ('VND', 'Vietnamese Dong', '₫', 0, true, 36),
    ('EGP', 'Egyptian Pound', 'E£', 2, true, 37),
    ('PKR', 'Pakistani Rupee', '₨', 2, true, 38),
    ('NGN', 'Nigerian Naira', '₦', 2, true, 39),
    ('BDT', 'Bangladeshi Taka', '৳', 2, true, 40),
    ('RON', 'Romanian Leu', 'lei', 2, true, 41),
    ('BGN', 'Bulgarian Lev', 'лв', 2, true, 42),
    ('HRK', 'Croatian Kuna', 'kn', 2, true, 43),
    ('ISK', 'Icelandic Krona', 'kr', 0, true, 44),
    ('TWD', 'Taiwan Dollar', 'NT$', 2, true, 45),
    ('UAH', 'Ukrainian Hryvnia', '₴', 2, true, 46),
    ('QAR', 'Qatari Riyal', '﷼', 2, true, 47),
    ('KWD', 'Kuwaiti Dinar', 'د.ك', 3, true, 48),
    ('BHD', 'Bahraini Dinar', '.د.ب', 3, true, 49),
    ('OMR', 'Omani Rial', '﷼', 3, true, 50)
ON CONFLICT (code) DO NOTHING;
