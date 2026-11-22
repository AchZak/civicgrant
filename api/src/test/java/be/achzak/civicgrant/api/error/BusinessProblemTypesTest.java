package be.achzak.civicgrant.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import be.achzak.civicgrant.core.fundingcall.domain.FundingCallErrorCode;
import be.achzak.civicgrant.core.grantapplication.domain.GrantApplicationErrorCode;
import be.achzak.civicgrant.core.organisation.domain.OrganisationErrorCode;
import be.achzak.civicgrant.core.sharedkernel.domain.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

@DisplayName("Business problem types")
class BusinessProblemTypesTest {

    private static final Path CONTRACT = Path.of("src/main/openapi/openapi.yaml");

    // A not-found code always answers Belgif's resourceNotFound, never a problem type of its own.
    private static final Set<ErrorCode> NOT_FOUND = Set.of(
            FundingCallErrorCode.FUNDING_CALL_NOT_FOUND,
            OrganisationErrorCode.ORGANISATION_NOT_FOUND,
            GrantApplicationErrorCode.APPLICATION_NOT_FOUND,
            GrantApplicationErrorCode.ASSESSMENT_NOT_FOUND,
            GrantApplicationErrorCode.DOCUMENT_NOT_FOUND);

    @ParameterizedTest(name = "{0}")
    @MethodSource("businessCodes")
    @DisplayName("names every business code in the contract")
    void namesEveryBusinessCodeInTheContract(ErrorCode code) throws IOException {
        // given
        String contract = Files.readString(CONTRACT);

        // when
        boolean named = Pattern.compile("[`:]" + DomainProblemHandler.problemName(code) + "`")
                .matcher(contract)
                .find();

        // then
        assertThat(named)
                .as("the contract names %s", DomainProblemHandler.problemName(code))
                .isTrue();
    }

    // Every enum implementing ErrorCode in the core, so a new aggregate's codes are checked without a list to update.
    static Stream<ErrorCode> businessCodes() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(ErrorCode.class));
        return scanner.findCandidateComponents("be.achzak.civicgrant.core").stream()
                .map(BeanDefinition::getBeanClassName)
                .map(BusinessProblemTypesTest::constantsOf)
                .flatMap(Arrays::stream)
                .filter(code -> !NOT_FOUND.contains(code));
    }

    private static ErrorCode[] constantsOf(String className) {
        try {
            return Class.forName(className).asSubclass(ErrorCode.class).getEnumConstants();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
