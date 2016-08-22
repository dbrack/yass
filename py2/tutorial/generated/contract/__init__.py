from enum import Enum
from typing import List, Any

import yass
from tutorial.base_types_external import Integer


# shows how to use contract internal base types

class ExpirationHandler(yass.BaseTypeHandler):
    def readBase(self, reader):
        return Expiration(
            reader.readZigZagInt(),
            reader.readZigZagInt(),
            reader.readZigZagInt()
        )

    def writeBase(self, value, writer):
        writer.writeZigZagInt(value.year)
        writer.writeZigZagInt(value.month)
        writer.writeZigZagInt(value.day)


class Expiration:
    TYPE_DESC = yass.TypeDesc(yass.FIRST_DESC_ID + 1, ExpirationHandler())

    def __init__(self, year, month, day):
        self.year = year
        self.month = month
        self.day = day

    def __str__(self):
        return '%s-%s-%s' % (self.year, self.month, self.day)


class PriceKind(Enum):
    BID = 0
    ASK = 1


class Price:
    def __init__(self):  # type: () -> None
        self.instrumentId = None  # type: Integer
        self.kind = None  # type: PriceKind
        self.value = None  # type: Integer


@yass.abstract
class Instrument:
    def __init__(self):  # type: () -> None
        self.id = None  # type: Integer
        self.name = None  # type: str


class SystemException(Exception):
    def __init__(self):  # type: () -> None
        self.message = None  # type: str


@yass.abstract
class ApplicationException(Exception):
    def __init__(self):  # type: () -> None
        pass


class UnknownInstrumentsException(ApplicationException):
    def __init__(self):  # type: () -> None
        ApplicationException.__init__(self)
        self.instrumentIds = None  # type: List[Integer]
        self.onlyNeededForTests1 = None  # type: Any
        self.onlyNeededForTests2 = None  # type: yass.Bytes
        self.onlyNeededForTests3 = None  # type: Exception


class Node:
    def __init__(self):  # type: () -> None
        self.id = None  # type: float
        self.links = None  # type: List[Node]


class EchoService:
    def echo(self, value):  # type: (Any) -> Any
        raise NotImplementedError()


class PriceEngine:
    def subscribe(self, instrumentIds):  # type: (List[Integer]) -> None
        raise NotImplementedError()


class PriceListener:
    def newPrices(self, prices):  # type: (List[Price]) -> None
        raise NotImplementedError()
