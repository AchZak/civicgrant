package be.achzak.civicgrant.domain.shared;

import java.io.Serializable;

// Each aggregate lists its failures in an enum implementing this. The name is the stable code the API turns into a
// problem type and the frontend translates, so renaming a constant is a breaking change.
public interface ErrorCode extends Serializable {

    String name();
}
