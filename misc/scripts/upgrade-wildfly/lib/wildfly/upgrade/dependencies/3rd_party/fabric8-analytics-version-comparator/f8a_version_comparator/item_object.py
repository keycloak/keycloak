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

"""Module to implement methods item types."""

from .base import Item
# TODO: setup logging


class IntegerItem(Item):
    """Integer Item class for maven version comparator tasks."""

    def __init__(self, str_version):
        """Initialize integer from string value of version.

        :str_version: part of version supplied as string
        """
        self.value = int(str_version)

    def int_cmp(self, cmp_value):
        """Compare two integers."""
        if self.value.__lt__(cmp_value):
            return -1
        if self.value.__gt__(cmp_value):
            return 1
        return 0

    def compare_to(self, item):
        """Compare two maven versions."""
        if item is None:
            return 0 if self.value == 0 else 1

        if isinstance(item, IntegerItem):
            return self.int_cmp(item.value)  # check if this value thing works
        if isinstance(item, StringItem):
            return 1
        if isinstance(item, ListItem):
            return 1
        else:
            raise ValueError("invalid item" + str(type(item)))

    def to_string(self):
        """Return string value of version."""
        return str(self.value)

    def __str__(self):
        """Return string value of version - Pythonish variant."""
        return str(self.value)


class StringItem(Item):
    """String Item class for maven version comparator tasks."""

    def __init__(self, str_version, followed_by_digit):
        """Initialize string value of version.

        :str_value: part of version supplied as string
        :followed_by_digit: True if str_version is followed by digit
        """
        self.qualifiers = ["alpha", "beta", "milestone", "rc", "snapshot", "", "sp"]

        self.aliases = {
               "ga": "",
               "final": "",
               "cr": "rc"
        }

        self.release_version_index = str(self.qualifiers.index(""))
        self._decode_char_versions(str_version, followed_by_digit)

    def _decode_char_versions(self, value, followed_by_digit):
        """Decode short forms of versions."""
        if followed_by_digit and len(value) == 1:
            if value.startswith("a"):
                value = "alpha"
            elif value.startswith("b"):
                value = "beta"
            elif value.startswith("m"):
                value = "milestone"

        self.value = self.aliases.get(value, value)

    def comparable_qualifier(self, qualifier):
        """Get qualifier that is comparable."""
        q_index = None
        if qualifier in self.qualifiers:
            q_index = self.qualifiers.index(qualifier)
        q_index_not_found = str(len(self.qualifiers)) + "-" + qualifier

        return str(q_index) if q_index is not None else q_index_not_found

    def str_cmp(self, val1, val2):
        """Compare two strings."""
        if val1.__lt__(val2):
            return -1
        if val1.__gt__(val2):
            return 1
        return 0

    def compare_to(self, item):
        """Compare two maven versions."""
        if item is None:
            temp = self.str_cmp(self.comparable_qualifier(self.value), self.release_version_index)
            return temp
        if isinstance(item, IntegerItem):
            return -1
        if isinstance(item, StringItem):
            return self.str_cmp(
                self.comparable_qualifier(
                    self.value), self.comparable_qualifier(
                    item.value))
        if isinstance(item, ListItem):
            return -1
        else:
            raise ValueError("invalid item" + str(type(item)))

    def to_string(self):
        """Return value in string form."""
        return str(self.value)

    def __str__(self):
        """Return string value of version - Pythonish variant."""
        return str(self.value)


class ListItem(Item):
    """List Item class for maven version comparator tasks."""

    def __init__(self):
        """Initialize string value of version."""
        self.array_list = list()

    def add_item(self, item):
        """Add item to array list."""
        self.array_list.append(item)

    def get_list(self):
        """Get object list items."""
        return self.array_list

    def normalize(self):
        """Remove trailing items: 0, "", empty list."""
        red_list = [0, None, ""]
        i = len(self.array_list) - 1
        while i >= 0:
            last_item = self.array_list[i]

            if not isinstance(last_item, ListItem):

                if last_item.value in red_list:
                    self.array_list.pop(i)
                else:
                    break

            i = i - 1

    def compare_to(self, item):
        """Compare two maven versions."""
        # TODO: reduce cyclomatic complexity
        if item is None:
            if len(self.array_list) == 0:
                return 0
            first = self.array_list[0]
            return first.compare_to(None)

        if isinstance(item, IntegerItem):
            return -1
        if isinstance(item, StringItem):
            return 1
        if isinstance(item, ListItem):
            left_iter = iter(self.array_list)
            right_iter = iter(item.get_list())

            while True:
                l_obj = next(left_iter, None)
                r_obj = next(right_iter, None)
                if l_obj is None and r_obj is None:
                    break
                result = 0
                if l_obj is None:
                    if r_obj is not None:
                        result = -1 * r_obj.compare_to(l_obj)
                else:
                    result = l_obj.compare_to(r_obj)
                if result != 0:
                    return result

            return 0
        else:
            raise ValueError("invalid item" + str(type(item)))
