/*
 * Copyright 2014 GoDataDriven B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

mapping {
    map duplicate() onto 'detectedDuplicate'
    map corrupt() onto 'detectedCorruption'
    map firstInSession() onto 'firstInSession'
    map timestamp() onto 'timestamp'
    map remoteHost() onto 'remoteHost'
    map referer() onto 'referer'
    map location() onto 'location'
    map viewportPixelWidth() onto 'viewportPixelWidth'
    map viewportPixelHeight() onto 'viewportPixelHeight'
    map screenPixelWidth() onto 'screenPixelWidth'
    map screenPixelHeight() onto 'screenPixelHeight'
    map partyId() onto 'partyId'
    map sessionId() onto 'sessionId'
    map pageViewId() onto 'pageViewId'
    map eventType() onto 'eventType'

    map userAgentString() onto 'userAgentString'
    def ua = userAgent()
    map ua.name() onto 'userAgentName'
    map ua.family() onto 'userAgentFamily'
    map ua.vendor() onto 'userAgentVendor'
    map ua.type() onto 'userAgentType'
    map ua.version() onto 'userAgentVersion'
    map ua.deviceCategory() onto 'userAgentDeviceCategory'
    map ua.osFamily() onto 'userAgentOsFamily'
    map ua.osVersion() onto 'userAgentOsVersion'
    map ua.osVendor() onto 'userAgentOsVendor'

    def locationUri = parse location() to uri

    /*
     * Only one forward slash in the path is some kind of overview or help page.
     * Just map the path before the .html onto the pageType. This covers:
     * overview-summary, overview-tree, deprecated-list, index-all, help-doc
     */
    def topLevelPageMatcher = match '^/([^/\\.]+)\\.html$' against locationUri.path()
    when topLevelPageMatcher.matches() apply {
      map topLevelPageMatcher.group(1) onto 'pageType'
      stop()
    }

    /*
     * Mapping for package-summary.
     */
    def packageSummaryMatcher = match '^/(.+?)/package-summary.html$' against locationUri.path()
    when packageSummaryMatcher.matches() apply {
      map 'package-summary' onto 'pageType'
      map packageSummaryMatcher.group(1) onto 'javaPackage'
      stop()
    }

    /*
     * Mapping for package-tree.
     */
    def packageTreeMatcher = match '^/(.+)/package-tree\\.html$' against locationUri.path()
    when packageTreeMatcher.matches() apply {
      map 'package-tree' onto 'pageType'
      map packageSummaryMatcher.group(1) onto 'javaPackage'
      stop()
    }

    /*
     * Mapping for class view.
     */
    def classMatcher = match '^/(.+)/(.+)\\.html$' against locationUri.path()
    when classMatcher.matches() apply {
      map 'class' onto 'pageType'
      map classMatcher.group(1) onto 'javaPackage'
      map classMatcher.group(2) onto 'javaType'
      stop()
    }
}
