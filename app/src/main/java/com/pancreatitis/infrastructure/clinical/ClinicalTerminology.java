package com.pancreatitis.infrastructure.clinical;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ClinicalTerminology {

    public static final Locale RU_LOCALE = Locale.forLanguageTag("ru-RU");

    public static final String CLASS_0_NAME = "Острый панкреатит: отёчная форма";
    public static final String CLASS_1_NAME = "Острый панкреатит: некротическая форма (панкреонекроз)";

    private static final Map<String, String> FEATURE_NAMES_WITH_SI;

    static {
        Map<String, String> map = new HashMap<>();

        map.put("sex", "Пол пациента");
        map.put("age", "Возраст пациента (полных лет)");
        map.put("ds naprav", "«Тревожный» диагноз направления");
        map.put("time disease", "Время от начала заболевания (часы / сутки)");
        map.put("chr pancr", "Хронический панкреатит в анамнезе");
        map.put("soput disease", "Сопутствующая патология");
        map.put("compens", "Компенсация сопутствующей патологии");
        map.put("pain", "Локализация болевого синдрома");
        map.put("intensiv", "Интенсивность боли (баллы 1-3)");
        map.put("rvota", "Рвота");
        map.put("«cofe gugha»", "Рвота «кофейной гущей»");
        map.put("cofe gugha", "Рвота «кофейной гущей»");
        map.put("slabost", "Эпизоды слабости, головокружения");
        map.put("ogirenie", "Ожирение (ИМТ)");
        map.put("t", "Температура тела (°C)");
        map.put("encef", "Признаки энцефалопатии");
        map.put("bled cut", "Бледность кожных покровов");
        map.put("mramor cut", "Мраморность кожных покровов");
        map.put("icterus", "Желтуха");
        map.put("hipermia face", "Гиперемия лица");
        map.put("ps", "Частота сердечных сокращений (уд/мин)");
        map.put("ad low", "Систолическое АД < 100 мм рт. ст.");
        map.put("kollaps", "Эпизоды сосудистого коллапса");
        map.put("chdd", "Частота дыхательных движений (в мин)");
        map.put("meteorism", "Метеоризм");
        map.put("pain of adbomin", "Болезненность живота при пальпации");
        map.put("rigid of abdomi", "Ригидность передней брюшной стенки");
        map.put("s. peritonit", "Симптом Щеткина-Блюмберга");
        map.put("er", "Эритроциты (10¹²/л)");
        map.put("hb", "Гемоглобин (г/л)");
        map.put("l", "Лейкоциты (10⁹/л)");
        map.put("lii", "Лейкоцитарный индекс интоксикации (отн. ед.)");
        map.put("coe", "Скорость оседания эритроцитов (СОЭ, мм/ч)");
        map.put("ht", "Гематокрит (л/л)");
        map.put("amilase", "Альфа-амилаза сыворотки крови (Ед/л)");
        map.put("amilase_urine", "Альфа-амилаза мочи (диастаза, Ед/л)");
        map.put("crb", "С-реактивный белок (СРБ, мг/л)");
        map.put("crp", "С-реактивный белок (СРБ, мг/л)");
        map.put("glu", "Глюкоза крови (ммоль/л)");
        map.put("albumin", "Общий белок сыворотки крови (г/л)");
        map.put("bil", "Билирубин общий сыворотки крови (мкмоль/л)");
        map.put("ur", "Мочевина сыворотки крови (ммоль/л)");
        map.put("creatinin", "Креатинин сыворотки крови (мкмоль/л)");
        map.put("creatinine", "Креатинин сыворотки крови (мкмоль/л)");
        map.put("potassium", "Калий сыворотки крови (ммоль/л)");
        map.put("sodium", "Натрий сыворотки крови (ммоль/л)");
        map.put("calcium", "Кальций сыворотки крови (ммоль/л)");
        map.put("gemolis", "Гемолиз, фибринолиз или липидемия");
        map.put("ecg", "ЭКГ: признаки ишемии или дистрофии");
        map.put("ls/cen gid", "Выпот в брюшной полости (мл)");
        map.put("ls steat", "Очаги стеатонекроза при лапароскопии");
        map.put("effect of treat", "Эффект от консервативной терапии");
        map.put("neutrophils_stab", "Палочкоядерные нейтрофилы (%)");

        FEATURE_NAMES_WITH_SI = Collections.unmodifiableMap(map);
    }

    private ClinicalTerminology() {
    }

    public static String formatClinicalFeatureName(String key, String fallbackOpis) {
        if (key != null) {
            String normalizedKey = key.trim().toLowerCase();
            if (FEATURE_NAMES_WITH_SI.containsKey(normalizedKey)) {
                return FEATURE_NAMES_WITH_SI.get(normalizedKey);
            }
        }
        if (fallbackOpis != null && !fallbackOpis.trim().isEmpty()) {
            String normalizedOpis = fallbackOpis.trim().toLowerCase();
            for (Map.Entry<String, String> entry : FEATURE_NAMES_WITH_SI.entrySet()) {
                if (normalizedOpis.contains(entry.getKey()) || entry.getValue().toLowerCase().contains(normalizedOpis)) {
                    return entry.getValue();
                }
            }
            return fallbackOpis;
        }
        return (key != null) ? key : "";
    }

    public static String getCanonicalClassName(int classIndex, String fallback) {
        if (classIndex == 0) {
            return CLASS_0_NAME;
        } else if (classIndex == 1) {
            return CLASS_1_NAME;
        }
        return (fallback != null && !fallback.isEmpty()) ? fallback : ("Форма #" + classIndex);
    }

    public static String formatPercentage(float fraction) {
        float percent = fraction * 100f;
        return String.format(RU_LOCALE, "%.1f%%", percent);
    }

    public static String formatDecimal(float value, int decimalPlaces) {
        String pattern = "%." + decimalPlaces + "f";
        return String.format(RU_LOCALE, pattern, value);
    }
}
