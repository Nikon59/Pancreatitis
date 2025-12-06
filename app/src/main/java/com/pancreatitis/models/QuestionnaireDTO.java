package com.pancreatitis.models;

public class QuestionnaireDTO extends Questionnaire {
    private int[] characteristicValues;

    public QuestionnaireDTO() {
        super();
    }

    public int[] getCharacteristicValues() {
        return characteristicValues;
    }

    public void setCharacteristicValues(int[] characteristicValues) {
        this.characteristicValues = characteristicValues;
    }
}
