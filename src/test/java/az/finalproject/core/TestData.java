package az.finalproject.core;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Test datası generatorları */
public final class TestData {

    private TestData() {
    }

    /** Format baxımından düzgün AZ IBAN: AZ + 2 rəqəm + 4 hərf + 20 simvol (28) */
    public static String iban(int number) {
        return String.format("AZ12NABZ%020d", number);
    }

    public static String repeat(char c, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(c);
        return sb.toString();
    }

    /** Kartın gözlənilən expiry dəyəri: bu ay / bu il + 3 (Bakı vaxtı), MM/YY */
    public static String expectedExpiry() {
        return YearMonth.now(ZoneId.of("Asia/Baku")).plusYears(3).format(DateTimeFormatter.ofPattern("MM/yy"));
    }
}
