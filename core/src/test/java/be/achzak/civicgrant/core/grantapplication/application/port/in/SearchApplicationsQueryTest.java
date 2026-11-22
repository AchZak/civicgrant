package be.achzak.civicgrant.core.grantapplication.application.port.in;

import static be.achzak.civicgrant.core.sharedkernel.domain.Actors.anApplicant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Search applications query")
class SearchApplicationsQueryTest {

    @ParameterizedTest
    @CsvSource({"1, 1", "1, 100", "2147483647, 20"})
    @DisplayName("accepts any page from 1 and a page size from 1 to 100")
    void acceptsAnyPageFromOneAndAPageSizeFromOneToHundred(int page, int pageSize) {
        // when
        SearchApplicationsQuery query = new SearchApplicationsQuery(Set.of(), Set.of(), page, pageSize, anApplicant());

        // then
        assertThat(query)
                .extracting(SearchApplicationsQuery::page, SearchApplicationsQuery::pageSize)
                .containsExactly(page, pageSize);
    }

    @ParameterizedTest
    @CsvSource({"0, 20", "1, 0", "1, 101"})
    @DisplayName("refuses a page below 1 or a page size outside 1 to 100")
    void refusesAPageBelowOneOrAPageSizeOutsideOneToHundred(int page, int pageSize) {
        // when
        Throwable thrown =
                catchThrowable(() -> new SearchApplicationsQuery(Set.of(), Set.of(), page, pageSize, anApplicant()));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
