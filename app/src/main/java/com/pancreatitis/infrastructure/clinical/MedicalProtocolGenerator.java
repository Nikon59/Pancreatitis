package com.pancreatitis.infrastructure.clinical;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.pancreatitis.core.model.ml.ModelMetadata;
import com.pancreatitis.models.CharasteristicDTO;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.modules.prediction.PredictionModule;
import com.pancreatitis.modules.prediction.PredictionResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MedicalProtocolGenerator {

    private static final String DISCLAIMER =
            "НОРМАТИВНЫЙ МЕДИЦИНСКИЙ ДИСКЛЕЙМЕР:\n" +
            "Данный протокол сформирован на основе расчёта математической модели оценки клинической формы острого панкреатита. " +
            "Результаты расчёта носят исключительно информационно-консультативный характер для лечащего врача, не являются " +
            "окончательным клиническим диагнозом и не могут служить единственным основанием для выбора тактики лечения или " +
            "хирургического вмешательства. Окончательное решение принимается лечащим врачом и клиническим консилиумом " +
            "в соответствии с действующими клиническими рекомендациями Минздрава России.";

    private MedicalProtocolGenerator() {
    }

    public static String buildProtocolText(
            Context context,
            Questionnaire questionnaire,
            Patient patient,
            List<CharasteristicDTO> characteristics,
            PredictionResult predictionResult
    ) {
        StringBuilder sb = new StringBuilder();
        Locale ruLocale = ClinicalTerminology.RU_LOCALE;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", ruLocale);
        String currentDateTime = dateFormat.format(new Date());

        sb.append("ПРОТОКОЛ ДИФФЕРЕНЦИАЛЬНОЙ ДИАГНОСТИКИ ОСТРОГО ПАНКРЕАТИТА\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("Дата и время формирования: ").append(currentDateTime).append(" (ru-RU)\n");

        String cardId = (questionnaire != null && questionnaire.getId() > 0)
                ? String.valueOf(questionnaire.getId())
                : "Новая анкета";
        sb.append("ID карты / анкеты: ").append(cardId).append("\n");

        if (patient != null && patient.getFio() != null && !patient.getFio().trim().isEmpty()) {
            sb.append("Пациент: ").append(patient.getFio().trim()).append("\n");
        }
        if (questionnaire != null && questionnaire.getAdmittedFrom() != null && !questionnaire.getAdmittedFrom().trim().isEmpty()) {
            sb.append("Отделение / поступление: ").append(questionnaire.getAdmittedFrom().trim()).append("\n");
        }
        sb.append("\n");

        // 1. Параметры математической модели
        sb.append("1. ПАРАМЕТРЫ МАТЕМАТИЧЕСКОЙ МОДЕЛИ:\n");
        PredictionModule pm = PredictionModule.getInstance();
        ModelMetadata activeMeta = pm.getActiveModelMetadata();
        String modelName = activeMeta != null ? activeMeta.getName() : pm.getActiveModelId();
        String modelId = activeMeta != null ? activeMeta.getId() : pm.getActiveModelId();
        String sha256 = activeMeta != null ? activeMeta.getSha256Checksum() : "N/A";
        String version = activeMeta != null ? activeMeta.getVersion() : "1.0.0";

        sb.append("• Активная модель: ").append(modelName).append(" (").append(modelId).append(")\n");
        sb.append("• Версия модели: ").append(version).append("\n");
        sb.append("• Контрольная сумма весов (SHA-256): ").append(sha256).append("\n\n");

        // 2. Ключевые показатели в единицах СИ
        sb.append("2. КЛЮЧЕВЫЕ КЛИНИКО-ЛАБОРАТОРНЫЕ ПОКАЗАТЕЛИ (в единицах СИ):\n");
        if (characteristics != null && !characteristics.isEmpty()) {
            for (CharasteristicDTO dto : characteristics) {
                String displayName = ClinicalTerminology.formatClinicalFeatureName(dto.getName(), dto.getOpis());
                float val = dto.getValue();
                String valStr;
                if (Math.abs(val - Math.round(val)) < 0.0001f) {
                    valStr = String.valueOf(Math.round(val));
                } else {
                    valStr = String.format(ruLocale, "%.2f", val);
                }
                sb.append("• ").append(displayName).append(": ").append(valStr).append("\n");
            }
        } else {
            sb.append("• Клинические показатели не заполнены\n");
        }
        sb.append("\n");

        // 3. Распределение вероятностей с разделителем-запятой
        sb.append("3. РАСЧЁТНАЯ ВЕРОЯТНОСТЬ ФОРМЫ ЗАБОЛЕВАНИЯ:\n");
        if (predictionResult != null && !predictionResult.getProbabilities().isEmpty()) {
            Map<Integer, Float> probs = predictionResult.getProbabilities();
            for (Map.Entry<Integer, Float> entry : probs.entrySet()) {
                int classIdx = entry.getKey();
                float prob = entry.getValue();
                String className = ClinicalTerminology.getCanonicalClassName(classIdx, null);
                float percent = prob * 100.0f;
                String probStr = String.format(ruLocale, "%.1f%% (%.3f)", percent, prob);
                sb.append("• ").append(className).append(" — ").append(probStr).append("\n");
            }
            int predictedClass = predictionResult.getPredictedClass();
            String predictedName = ClinicalTerminology.getCanonicalClassName(predictedClass, predictionResult.getPredictedClassName());
            sb.append("Заключение модели: ").append(predictedName).append("\n");
        } else {
            sb.append("• Расчётная вероятность не вычислена\n");
        }
        sb.append("\n");

        // 4. Нормативный медицинский дисклеймер
        sb.append("4. ").append(DISCLAIMER).append("\n");

        return sb.toString();
    }

    public static void copyProtocolToClipboardAndNotify(
            Context context,
            View snackbarAnchor,
            Questionnaire questionnaire,
            Patient patient,
            List<CharasteristicDTO> characteristics,
            PredictionResult predictionResult
    ) {
        String protocolText = buildProtocolText(context, questionnaire, patient, characteristics, predictionResult);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Медицинский протокол", protocolText);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }

        String message = "Медицинский протокол скопирован в буфер обмена";
        if (snackbarAnchor != null) {
            Snackbar.make(snackbarAnchor, message, Snackbar.LENGTH_LONG).show();
        }
    }
}
