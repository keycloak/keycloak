# Copyright © 2018 Red Hat Inc.
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
# Author: Geetika Batra <gbatra@redhat.com>
#

"""Module to implement Comparable Version class."""

import typing

from .item_object import IntegerItem
from .item_object import StringItem
from .item_object import ListItem


class ComparableVersion:
    """Class for Comparable Version."""

    def __init__(self, version: str):
        """Initialize comparable version class.

        :version: Version supplied as a string
        """
        if not isinstance(version, str):
            raise TypeError(
                "Invalid type {got!r} of argument `version`, expected {expected!r}".format(
                    got=type(version),
                    expected=str
                ))

        self.version = version
        self.items = self.parse_version()

    def __repr__(self):
        """Return representation of ComparableVersion object."""
        return "{cls!s}(version={version!r})".format(
            cls=self.__class__.__name__,
            version=self.version
        )

    def __str__(self):
        """Return version string held by ComparableVersion object."""
        return "{version!s}".format(
            version=self.version
        )

    def __eq__(self, other):
        """Compare ComparableVersion objects for equality.

        This rich comparison implies whether self == other
        """
        # don't call compare_to(None)
        if other is None:
            return False

        return self.compare_to(other) == 0

    def __ne__(self, other):
        """Compare ComparableVersion objects for equality.

        This rich comparison implies whether self != other
        """
        # don't call compare_to(None)
        if other is None:
            return True

        return self.compare_to(other) != 0

    def __lt__(self, other):
        """Compare ComparableVersion objects.

        This rich comparison implies whether self < other
        """
        # don't call compare_to(None)
        if other is None:
            return False

        return self.compare_to(other) == -1

    def __le__(self, other):
        """Compare ComparableVersion objects.

        This rich comparison implies whether self <= other
        """
        # don't call compare_to(None)
        if other is None:
            return False

        return self.compare_to(other) <= 0

    def __gt__(self, other):
        """Compare ComparableVersion objects.

        This rich comparison implies whether self > other
        """
        # don't call compare_to(None)
        if other is None:
            return True

        return self.compare_to(other) == 1

    def __ge__(self, other):
        """Compare ComparableVersion objects.

        This rich comparison implies whether self >= other
        """
        # don't call compare_to(None)
        if other is None:
            return True

        return self.compare_to(other) >= 0

    def parse_version(self):
        """Parse version."""
        # TODO: reduce cyclomatic complexity
        ref_list = ListItem()
        items = ref_list
        parse_stack = list()
        version = self.version.lower()
        parse_stack.append(ref_list)
        _is_digit = False

        _start_index = 0

        for _ch in range(0, len(version)):

            ver_char = version[_ch]

            if ver_char == ".":

                if _ch == _start_index:
                    ref_list.add_item(IntegerItem(0))
                else:
                    ref_list.add_item(self.parse_item(_is_digit, version[_start_index: _ch]))

                _start_index = _ch + 1

            elif ver_char == "-":
                if _ch == _start_index:
                    ref_list.add_item(IntegerItem(0))
                else:
                    ref_list.add_item(self.parse_item(_is_digit, version[_start_index: _ch]))
                _start_index = _ch + 1

                temp = ListItem()
                ref_list.add_item(temp)
                ref_list = temp
                parse_stack.append(ref_list)
            elif ver_char.isdigit():
                if not _is_digit and _ch > _start_index:
                    ref_list.add_item(StringItem(version[_start_index: _ch], True))
                    _start_index = _ch

                    temp = ListItem()
                    ref_list.add_item(temp)
                    ref_list = temp
                    parse_stack.append(ref_list)
                _is_digit = True
            else:
                if _is_digit and _ch > _start_index:
                    ref_list.add_item(self.parse_item(True, version[_start_index:_ch]))
                    _start_index = _ch
                    temp = ListItem()
                    ref_list.add_item(temp)
                    ref_list = temp
                    parse_stack.append(ref_list)
                _is_digit = False

        if len(version) > _start_index:
            ref_list.add_item(self.parse_item(_is_digit, version[_start_index:]))

        while parse_stack:
            ref_list = parse_stack.pop()
            ref_list.normalize()

        return items

    @staticmethod
    def parse_item(_is_digit, buf):
        """Wrap items in version in respective object class."""
        # TODO: make this function static (it does not need 'self')
        if _is_digit:
            return IntegerItem(buf)

        return StringItem(buf, False)

    def compare_to(self, obj: typing.Union["ComparableVersion", str]):
        """Compare two ComparableVersion objects."""
        if isinstance(obj, ComparableVersion):
            # compare two objects of the same type
            cmp_result = self.items.compare_to(obj.items)
        elif isinstance(obj, str):
            # compare against string
            cmp_result = self.items.compare_to(ComparableVersion(obj).items)
        else:
            raise TypeError(
                "Invalid type {got!r} of argument `obj`, expected <{expected}>".format(
                    got=type(obj),
                    expected=typing.Union["ComparableVersion", str]
                ))

        return cmp_result
