// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Author {
    @JsonProperty("lastName")
    private String lastName;
    @JsonProperty("firstName")
    private String firstName;

    Author() {
    }

    Author(String firstName, String lastName) {
        this.lastName = lastName;
        this.firstName = firstName;
    }

    /**
     * Returns the author's last name.
     *
     * @return string with last name.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Returns the author's first name.
     *
     * @return string with first name.
     */
    public String getFirstName() {
        return firstName;
    }

 /*   private void setLastName(String lastName) {
        this.lastName = lastName;
    }*/

    private void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns a String of the author's name in teh format "lastName, firstName"
     *
     * @return String with the correct format to print author's name
     */
    public String toString() {
        return lastName + ", " + firstName;
    }
}
