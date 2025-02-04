package org.ushastoe.fluffy.helpers;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

public class BirthdayHelper {
    public static String getZodiacSign(int month, int day) {
        int zodiacResId;

        if ((month == 12 && day >= 22) || (month == 1 && day <= 19)) {
            zodiacResId = R.string.zodiac_capricorn;
        } else if ((month == 1 && day >= 20) || (month == 2 && day <= 18)) {
            zodiacResId = R.string.zodiac_aquarius;
        } else if ((month == 2 && day >= 19) || (month == 3 && day <= 20)) {
            zodiacResId = R.string.zodiac_pisces;
        } else if ((month == 3 && day >= 21) || (month == 4 && day <= 19)) {
            zodiacResId = R.string.zodiac_aries;
        } else if ((month == 4 && day >= 20) || (month == 5 && day <= 20)) {
            zodiacResId = R.string.zodiac_taurus;
        } else if ((month == 5 && day >= 21) || (month == 6 && day <= 20)) {
            zodiacResId = R.string.zodiac_gemini;
        } else if ((month == 6 && day >= 21) || (month == 7 && day <= 22)) {
            zodiacResId = R.string.zodiac_cancer;
        } else if ((month == 7 && day >= 23) || (month == 8 && day <= 22)) {
            zodiacResId = R.string.zodiac_leo;
        } else if ((month == 8 && day >= 23) || (month == 9 && day <= 22)) {
            zodiacResId = R.string.zodiac_virgo;
        } else if ((month == 9 && day >= 23) || (month == 10 && day <= 22)) {
            zodiacResId = R.string.zodiac_libra;
        } else if ((month == 10 && day >= 23) || (month == 11 && day <= 21)) {
            zodiacResId = R.string.zodiac_scorpio;
        } else if ((month == 11 && day >= 22) || (month == 12 && day <= 21)) {
            zodiacResId = R.string.zodiac_sagittarius;
        } else {
            return "";
        }

        return LocaleController.getString(zodiacResId);
    }
}
