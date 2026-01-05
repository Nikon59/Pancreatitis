package com.pancreatitis.models;

import java.util.List;

public class QuestionnaireDTO extends Questionnaire {
    private List<CharasteristicDTO> characteristicValues;

    public QuestionnaireDTO() {
        super();
    }
    public QuestionnaireDTO(Questionnaire questionnaire) {
        super(questionnaire);
    }

    public List<CharasteristicDTO> getCharacteristicValues() {
        return characteristicValues;
    }

    public void setCharacteristicValues(List<CharasteristicDTO> characteristicValues) {
        this.characteristicValues = characteristicValues;
    }
}
