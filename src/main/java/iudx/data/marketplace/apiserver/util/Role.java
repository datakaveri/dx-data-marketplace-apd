package iudx.data.marketplace.apiserver.util;

import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;

import java.util.stream.Stream;

import static iudx.data.marketplace.common.ResponseUrn.ROLE_NOT_FOUND;

public enum Role {
    PROVIDER("provider"),
    CONSUMER_DELEGATE("consumerDelegate"),
    PROVIDER_DELEGATE("providerDelegate"),
    CONSUMER("consumer");
    private final String role;

    Role(String value) {
        role = value;
    }
    public String getRole() {
        return role;
    }

    public static Role fromString(String roleValue) {
        return Stream.of(values())
                .filter(element -> element.getRole().equalsIgnoreCase(roleValue))
                .findAny()
                .orElseThrow(() -> new DxRuntimeException(404, ROLE_NOT_FOUND));
    }
}
