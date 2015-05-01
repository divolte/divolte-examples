from time import time
from collections import defaultdict
import sys

class RunningAverage:
    # refreshrate and period in seconds
    def __init__(self, refreshRate, period):
        self.refreshRate = refreshRate
        self.period = period

        self.nbElements = self.period / self.refreshRate + 2
        self.lastUpdate = int(time() / self.refreshRate)
        self.values = [0] * self.nbElements

    def addValue(self, timestamp, value):
        if timestamp < (self.lastUpdate - self.period):
            if __debug__:
                print >> sys.stderr, "too old, ignoring"
        else:
            # we get the current time factor
            timeFactor = int(timestamp / self.refreshRate)

            # we first _update the buffer
            self._update(timeFactor)

            # and then we add our value to the current element
            self.values[(timeFactor % self.nbElements)] += value

    def getAverage(self):
        return self.getSum() / float(self.period)

    def getSum(self):
        # We get the current timeFactor
        timeFactor = int(time() / self.refreshRate)

        # We first _update the buffer
        self._update(timeFactor)

        # The sum of all elements used for the average.
        sum = 0

        # Starting on oldest one (the one after the next one)
        # Ending on last one fully updated (the one previous current one)
        for i in range(timeFactor + 2, timeFactor + self.nbElements + 1):
            # Simple addition
            sum += self.values[i % self.nbElements]

        return sum

    def getAverageAsDict(self):
        sumdict = self.getSumAsDict()
        for key, value in sumdict.items():
            sumdict[key] = value / float(self.refreshRate)

        return sumdict

    def getSumAsDict(self):
        # We get the current timeFactor
        timeFactor = int(time() / self.refreshRate)

        # We first _update the buffer
        self._update(timeFactor)

        sumdict = {}

        # Starting on oldest one (the one after the next one)
        # Ending on last one fully updated (the one previous current one)
        for i in range(timeFactor, timeFactor - self.nbElements, -1):
            sumdict[i * self.refreshRate] = self.values[i % self.nbElements]

        return sumdict

    def _update(self, timeFactor):
        # If we have a really OLD lastUpdate, we could erase the buffer a
        # huge number of times, so if it's really old, we change it so we'll only
        # erase the buffer once.

        if self.lastUpdate < timeFactor - self.nbElements:
            self.lastUpdate = timeFactor - self.nbElements - 1;

        # For all values between lastUpdate + 1 (next value than last updated)
        # and timeFactor (which is the new value insertion position)
        if self.lastUpdate < timeFactor:
            for i in range(self.lastUpdate + 1, timeFactor):
                self.values[i % self.nbElements] = 0

            # We also clear the next value to be inserted (so on next time change...)
            self.values[(timeFactor + 1) % self.nbElements] = 0

            # And we _update lastUpdate.
            self.lastUpdate = timeFactor


class StringRunningAverage:

    def __init__(self, refreshRate, period):
        self.period = period
        self.values = defaultdict(lambda: RunningAverage(refreshRate, period))

    def addValue(self, timestamp, value):
        self.values[value].addValue(timestamp, 1)

    def getAverage(self):
        return self.getSum() / float(self.period)

    def getSum(self):
        return sum(value.getSum() for value in self.values.itervalues())

    def getAverageAsDict(self):
        return dict((key, value.getAverage()) for key, value in self.values.iteritems())

    def getSumAsDict(self):
        return dict((key, value.getSum()) for key, value in self.values.iteritems())
