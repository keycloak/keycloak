function getGroupIds(user) {
  var groupIds = [];
  var groupsStream = user.getGroupsStream();

  if (groupsStream != null) {
    var groupIterator = groupsStream.iterator();
    while (groupIterator.hasNext()) {
      var group = groupIterator.next();
      groupIds.push(group.getId());
    }
  }

  return groupIds;
}

var groupIds = getGroupIds(user);

token.setOtherClaims("group_ids",
  Java.to(groupIds, "java.lang.String[]")
);