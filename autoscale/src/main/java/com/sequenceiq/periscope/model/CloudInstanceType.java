package com.sequenceiq.periscope.model;

public class CloudInstanceType {

    private String instanceName;

    private Integer coreCPU;

    private Long memoryInMb;

    public CloudInstanceType(String instanceName, Integer numCores, Long memoryInMb) {
        this(numCores, memoryInMb);
        this.instanceName = instanceName;
    }

    public CloudInstanceType(Integer numCores, Long memoryInMb) {
        this.coreCPU = numCores;
        this.memoryInMb = memoryInMb;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public Integer getCoreCPU() {
        return coreCPU;
    }

    public void setCoreCPU(Integer coreCPU) {
        this.coreCPU = coreCPU;
    }

    public Long getMemoryInMB() {
        return memoryInMb;
    }
}
