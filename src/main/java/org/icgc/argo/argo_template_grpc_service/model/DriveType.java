package org.icgc.argo.argo_template_grpc_service.model;

import lombok.NonNull;

import java.util.stream.Stream;

import static java.lang.String.format;

public enum DriveType {
    FRONT,
    BACK,
    ALL;

    public static DriveType resolveDriveType (@NonNull String typeName){
        return Stream.of(values())
            .filter(x -> x.name().equals(typeName))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(format("The drive type '%s' cannot be resolved", typeName)));
    }



}
