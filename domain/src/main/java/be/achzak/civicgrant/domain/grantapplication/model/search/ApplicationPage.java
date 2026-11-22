package be.achzak.civicgrant.domain.grantapplication.model.search;

import java.util.List;
import java.util.Objects;

public record ApplicationPage(List<ApplicationSummary> items, int page, int pageSize, long total) {

    public ApplicationPage {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
    }
}
