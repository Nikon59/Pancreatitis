package com.pancreatitis.modules.bip39;

/**
 * Утилита для восстановления зашифрованного ключа из BIP39-мнемоники.
 * Может использоваться в клиентском приложении для входа.
 */
public class Bip39Decoder {
    /**
     * Восстанавливает исходный массив байтов (16 байт) из 12-словной BIP39 фразы.
     *
     * @param mnemonic мнемоническая фраза (12 слов через пробел)
     * @return зашифрованный ключ (encrypt_user_key)
     * @throws IllegalArgumentException если мнемоника некорректна (неизвестные слова,
     *                                  неверная длина, ошибка контрольной суммы)
     */
    public static byte[] fromMnemonic(String mnemonic) {
        return Bip39Utils.recoverEntropy(mnemonic);
    }
}