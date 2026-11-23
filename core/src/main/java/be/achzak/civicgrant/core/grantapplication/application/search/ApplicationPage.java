package be.achzak.civicgrant.core.grantapplication.application.search;

import java.util.List;

public record ApplicationPage(List<ApplicationSummary> items, int page, int pageSize, long total) {

    public ApplicationPage {
        items = List.copyOf(items);
    }
}
