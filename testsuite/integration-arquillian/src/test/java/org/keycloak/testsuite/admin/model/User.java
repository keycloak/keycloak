/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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

package org.keycloak.testsuite.admin.model;

/**
 *
 * @author Filip Kiss
 */
public class User {

    private String userName;
	
	private String password;

    private String email;

    private String firstName;

    private String lastName;

    private boolean userEnabled;

    private boolean emailVerified;

    private String requiredUserActions;

    public User() {
        this.userEnabled = true;
        this.emailVerified = false;
    }

    public User(String userName) {
		this();
		this.userName = userName;
    }
	
	public User(String userName, String password) {
		this(userName);
		this.password = password;
    }

    public User(String userName, String password, String email) {
		this(userName, password);
		this.email = email;
    }

    public User(String userName, String password, String email, String firstName, String lastName) {
		this(userName, password, email);
		this.firstName = firstName;
        this.lastName = lastName;
    }

    public User(String userName, String password, String email, String firstName, String lastName, boolean userEnabled, boolean emailVerified, String requiredUserActions) {
		this(userName, password, email, firstName, lastName);
		this.requiredUserActions = requiredUserActions;
    }
	
	public User(User user) {
		this(user.userName, user.password, user.email, user.firstName, user.lastName,
				user.userEnabled, user.emailVerified, user.requiredUserActions);
	}

    public String getUserName() { return userName; }

    public void setUserName(String userName) { this.userName = userName; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }

    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }

    public void setLastName(String lastName) { this.lastName = lastName; }

    public boolean isUserEnabled() { return userEnabled; }

    public void setUserEnabled(boolean userEnabled) { this.userEnabled = userEnabled; }

    public boolean isEmailVerified() { return emailVerified; }

    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getRequiredUserActions() { return requiredUserActions; }

    public void setRequiredUserActions(String requiredUserActions) { this.requiredUserActions = requiredUserActions; }

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (emailVerified != user.emailVerified) return false;
        if (userEnabled != user.userEnabled) return false;
        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        if (firstName != null ? !firstName.equals(user.firstName) : user.firstName != null) return false;
        if (lastName != null ? !lastName.equals(user.lastName) : user.lastName != null) return false;
        if (requiredUserActions != null ? !requiredUserActions.equals(user.requiredUserActions) : user.requiredUserActions != null)
            return false;
        if (!userName.equals(user.userName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = userName.hashCode();
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (userEnabled ? 1 : 0);
        result = 31 * result + (emailVerified ? 1 : 0);
        result = 31 * result + (requiredUserActions != null ? requiredUserActions.hashCode() : 0);
        return result;
    }
}