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
