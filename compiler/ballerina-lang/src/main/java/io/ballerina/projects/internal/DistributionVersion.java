/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.projects.internal;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import io.ballerina.projects.ProjectException;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Distribution version parser that supports A.B.C and A.B.C.D.
 *
 * @since 2201.13.2
 */
public final class DistributionVersion {
    private static final Pattern FOUR_PART_VERSION_PATTERN = Pattern.compile(
            "^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
                    + "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$");

    private final Version semVer;
    private final int major;
    private final int minor;
    private final int patch;
    private final int fourth;

    private DistributionVersion(Version semVer, int major, int minor, int patch, int fourth) {
        this.semVer = semVer;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.fourth = fourth;
    }

    public static DistributionVersion from(String versionString) {
        try {
            Version semVer = Version.valueOf(versionString);
            return new DistributionVersion(semVer, semVer.getMajorVersion(), semVer.getMinorVersion(),
                    semVer.getPatchVersion(), 0);
        } catch (IllegalArgumentException e) {
            throw new ProjectException("Version cannot be empty");
        } catch (ParseException e) {
            Matcher matcher = FOUR_PART_VERSION_PATTERN.matcher(versionString);
            if (!matcher.matches()) {
                throw new ProjectException("Invalid version: '" + versionString + "'. " + e.toString());
            }

            try {
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));
                int patch = Integer.parseInt(matcher.group(3));
                int fourth = Integer.parseInt(matcher.group(4));
                StringBuilder semVerText = new StringBuilder()
                        .append(major).append('.').append(minor).append('.').append(patch);
                if (matcher.group(5) != null) {
                    semVerText.append('-').append(matcher.group(5));
                }
                if (matcher.group(6) != null) {
                    semVerText.append('+').append(matcher.group(6));
                }
                Version semVer = Version.valueOf(semVerText.toString());
                return new DistributionVersion(semVer, major, minor, patch, fourth);
            } catch (NumberFormatException ex) {
                throw new ProjectException("Invalid version: '" + versionString + "'. " + e.toString());
            } catch (ParseException | IllegalArgumentException ex) {
                throw new ProjectException("Invalid version: '" + versionString + "'. " + ex.toString());
            }
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

    public String toSemanticVersionString() {
        return semVer.toString();
    }

    public boolean greaterThan(DistributionVersion other) {
        Objects.requireNonNull(other);

        if (this.major != other.major) {
            return this.major > other.major;
        }
        if (this.minor != other.minor) {
            return this.minor > other.minor;
        }
        if (this.patch != other.patch) {
            return this.patch > other.patch;
        }
        if (this.fourth != other.fourth) {
            return this.fourth > other.fourth;
        }
        return this.semVer.greaterThan(other.semVer);
    }
}
