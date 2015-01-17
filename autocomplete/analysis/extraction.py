#
# Copyright 2015 GoDataDriven B.V.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import sys

def search_fragments(session):
    def fragment_dict(
            start_time,
            phrase,
            num_results,
            time_to_first_result,
            time_to_next_search,
            event_count,
            results):
        return {
                    'start_time': start_time,
                    'phrase': phrase,
                    'num_results': num_results,
                    'time_to_first_result': None if time_to_first_result == sys.maxint else time_to_first_result,
                    'time_to_next_search': None if time_to_next_search == sys.maxint else time_to_next_search,
                    'event_count': event_count,
                    'results': results
                  }

    phrase = None
    start_time = sys.maxint
    num_results = 0
    time_to_first_result = sys.maxint
    event_count = 0
    results = []
    
    for event in session:
        if (event['pageType'] == 'search') and (event['searchPhrase'] != phrase):
            if phrase:
                yield fragment_dict(start_time, phrase, num_results, time_to_first_result,
                                    event['timestamp'] - start_time, event_count, results)
                
            start_time = event['timestamp']
            phrase = event['searchPhrase']
            num_results = 0
            time_to_first_result = sys.maxint
            event_count = 0
            results = []
        elif event['pageType'] != 'search' and event['isSearchResult']:
            num_results += 1
            time_to_first_result = min(time_to_first_result, event['timestamp'] - start_time)
            results += [event['location']]

        event_count += 1
    
    if phrase:
        yield fragment_dict(start_time, phrase, num_results, time_to_first_result,
                            sys.maxint, event_count, results)
