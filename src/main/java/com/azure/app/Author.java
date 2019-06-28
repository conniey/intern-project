// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

public class Author {
    private String lastName;
    private String firstName;

    Author(String lastName, String firstName) {
        this.lastName = lastName;
        this.firstName = firstName;
    }

    /*
     * Returns the author's last name.
     *
     * @return  string with last name.
     */
    public String getLastName() {
        return lastName;
    }

    /*
     * Returns the author's first name.
     *
     * @return string with first name.
     */
    public String getFirstName() {
        return firstName;
    }
}
