package com.pancreatitis.core.storage;

import java.nio.file.Path;

public interface StoragePathProvider {

    Path getBaseDataDirectory();

    Path getModelsDirectory();

    Path getDatabasePath();

    Path getKeysDirectory();

    Path getTempDirectory();
}
