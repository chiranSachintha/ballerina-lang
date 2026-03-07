/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.projects;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a semantic version according to the semvar specification.
 *
 * @since 2.0.0
 */
public class SemanticVersion {
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(\\d+)\\.(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
                    + "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$");
    private static final int DEFAULT_FOURTH_PART = 0;

    private final Version semVer;
    private final int major;
    private final int minor;
    private final int patch;
    private final int fourthPart;
    private final String originalVersion;

    private SemanticVersion(Version semVer, int major, int minor, int patch, int fourthPart, String originalVersion) {
        this.semVer = semVer;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.fourthPart = fourthPart;
        this.originalVersion = originalVersion;
    }

    public static SemanticVersion from(String versionString) {
        if (versionString == null || versionString.trim().isEmpty()) {
            throw new ProjectException("Version cannot be empty");
        }

        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (!matcher.matches()) {
            throw new ProjectException("Invalid version: '" + versionString + "'.");
        }

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = Integer.parseInt(matcher.group(3));
            String fourthPartString = matcher.group(4);
            int fourthPart = fourthPartString != null ? Integer.parseInt(fourthPartString) : DEFAULT_FOURTH_PART;

            StringBuilder semVerText = new StringBuilder()
                    .append(major).append('.').append(minor).append('.').append(patch);
            if (matcher.group(5) != null) {
                semVerText.append('-').append(matcher.group(5));
            }
            if (matcher.group(6) != null) {
                semVerText.append('+').append(matcher.group(6));
            }

            Version semVer = Version.valueOf(semVerText.toString());
            return new SemanticVersion(semVer, major, minor, patch, fourthPart, versionString);
        } catch (NumberFormatException e) {
            throw new ProjectException("Invalid version: '" + versionString + "'. " + e.getMessage());
        } catch (ParseException e) {
            throw new ProjectException("Invalid version: '" + versionString + "'. " + e.toString());
        }
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public int fourthPart() {
        return fourthPart;
    }

    public String preReleasePart() {
        return semVer.getPreReleaseVersion();
    }

    public String buildMetadata() {
        return semVer.getBuildMetadata();
    }

    public boolean isStable() {
        if (this.major() == 0) {
            return false;
        }

        return !isPreReleaseVersion();
    }

    public boolean isPreReleaseVersion() {
        String preReleaseComp = semVer.getPreReleaseVersion();
        return preReleaseComp != null && !preReleaseComp.trim().isEmpty();
    }

    public boolean isInitialVersion() {
        return this.major() == 0;
    }

    public boolean greaterThan(SemanticVersion other) {
        return this.comparePrecedence(other) > 0;
    }

    public boolean greaterThanOrEqualTo(SemanticVersion other) {
        return this.comparePrecedence(other) >= 0;
    }

    public boolean lessThan(SemanticVersion other) {
        return this.comparePrecedence(other) < 0;
    }

    public boolean lessThanOrEqualTo(SemanticVersion other) {
        return this.comparePrecedence(other) <= 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        SemanticVersion otherSemVer = (SemanticVersion) other;
        return Objects.equals(originalVersion, otherSemVer.originalVersion);
    }

    @Override
    public String toString() {
        return originalVersion;
    }

    @Override
    public int hashCode() {
        return originalVersion.hashCode();
    }

    public VersionCompatibilityResult compareTo(SemanticVersion other) {
        Objects.requireNonNull(other);

        if (this.equals(other)) {
            return VersionCompatibilityResult.EQUAL;
        }

        if (this.major() != other.major()) {
            return VersionCompatibilityResult.INCOMPATIBLE;
        }

        if (this.isInitialVersion() && (this.minor() != other.minor())) {
            return VersionCompatibilityResult.INCOMPATIBLE;
        }

        // We've eliminated initial versions and versions with different major component.
        // Now we just need to check minor, patch and pre-release components.
        int result = this.comparePrecedence(other);
        if (result < 0) {
            return VersionCompatibilityResult.LESS_THAN;
        } else {
            return VersionCompatibilityResult.GREATER_THAN;
        }
    }

    /**
     * Represents the version compatibility between two {@code SemanticVersion} instances.
     *
     * @since 2.0.0
     */
    public enum VersionCompatibilityResult {
        INCOMPATIBLE,
        EQUAL,
        LESS_THAN,
        GREATER_THAN
    }

    private int comparePrecedence(SemanticVersion other) {
        Objects.requireNonNull(other);

        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        if (this.patch != other.patch) {
            return Integer.compare(this.patch, other.patch);
        }
        if (this.fourthPart != other.fourthPart) {
            return Integer.compare(this.fourthPart, other.fourthPart);
        }

        return this.semVer.compareTo(other.semVer);
    }
}
