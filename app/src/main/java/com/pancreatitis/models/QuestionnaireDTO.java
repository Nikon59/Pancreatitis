package com.pancreatitis.models;

public class QuestionnaireDTO extends Questionnaire {
    private float[] characteristicValues;

    public QuestionnaireDTO() {
        super();
    }

    public float[] getCharacteristicValues() {
        return characteristicValues;
    }

    public void setCharacteristicValues(float[] characteristicValues) {
        this.characteristicValues = characteristicValues;
    }
}
