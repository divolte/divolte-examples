from unittest import TestCase
from utils import RunningAverage, StringRunningAverage
from time import time

class TestUtils(TestCase):

    def setUp(self):
        self.r = RunningAverage(5, 60)

    def test_running_average(self):
        self.r.addValue(time(), 1)
        self.r.addValue(time(), 1)

        assert self.r.getSum() == 2, self.r.getSum()
        assert self.r.getAverage() == 2 / 60.0, self.r.getAverage()

    def test_running_average_bin(self):
        now = int(time() / 5) * 5
        self.r.addValue(now, 1)
        self.r.addValue(now, 1)
        self.r.addValue(now - 5, 1)

        assert self.r.getSumAsDict().get(now) == 2, (now, self.r.getSumAsDict())
        assert self.r.getAverageAsDict().get(now) == 2 / 5.0, (now, self.r.getAverageAsDict())

    def test_running_average_bool(self):
        self.r.addValue(time(), True)
        self.r.addValue(time(), True)

        assert self.r.getSum() == 2, self.r.getSum()
        assert self.r.getAverage() == 2 / 60.0, self.r.getAverage()

    def test_running_average_to_old(self):
        self.r.addValue(0, 1)
        self.r.addValue(0, 1)

        assert self.r.getSum() == 0, self.r.getSum()
        assert self.r.getAverage() == 0, self.r.getAverage()

class TestStringRA(TestCase):

    def setUp(self):
        self.r = StringRunningAverage(5, 60)
        self.r.addValue(time(), "hello")
        self.r.addValue(time(), "world")

    def test_stringrunning_average(self):
        assert self.r.getSum() == 2, self.r.getSum()
        assert self.r.getAverage() == 2 / 60.0, self.r.getAverage()

    def test_stringrunning_average_bin(self):
        self.r.addValue(time(), "world")

        assert self.r.getSumAsDict()["hello"] == 1, self.r.getSumAsDict()
        assert self.r.getSumAsDict()["world"] == 2, self.r.getSumAsDict()
        assert self.r.getAverageAsDict()["hello"] == 1 / 60.0, self.r.getAverageAsDict()
        assert self.r.getAverageAsDict()["world"] == 2 / 60.0, self.r.getAverageAsDict()
