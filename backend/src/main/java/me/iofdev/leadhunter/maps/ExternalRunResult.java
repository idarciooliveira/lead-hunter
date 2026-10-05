package me.iofdev.leadhunter.maps;

import java.math.BigDecimal;

public interface ExternalRunResult {
    String externalRunId();

    String datasetId();

    BigDecimal costUsd();
}
