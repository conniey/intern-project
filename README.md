# Book inventory console app

# Overview

The book inventory application can do multiple things, it can store books, search books, list books and delete books.
When the application is started, a menu is displayed with the following options:

__Title Screen__
Welcome! Select one of the options below (1 - 5).
1. [List books](#list-books)
1. [Add a book](#add-a-book)
1. [Find a book](#find-a-book)
1. [Delete book](#delete-a-book)
1. Quit

While the option is not "Quit", the application performs the operation and heads back to the title screen.

# List books

When "List books" is selected, the application begins listing all of the books in storage.
* Books are listed in alphabetical order, ascending, by last name then first name.

## Example

Here are all the books you have:
1. Dahl, Ronald - James and the Giant Peach
1. Rowling, J.K. - Harry Potter and the Philosopher's Stone
1. Rowling, Zachery - The Tombs
...

# Add a book

The screen should prompt the user for the following inputs:
1. Title?
1. Author?
1. Cover image?
    * Let user enter a path to an image file. (ie. C:\downloads\bookcover.png)
    * Validate that the path is *.png, *.jpg, or *.gif.
1. Save? Enter 'Y' or 'N'.
    1. If 'Yes', upload the image to Azure Blob Storage. Remember the path it was uploaded to.
    1. Save the entry in Azure Cosmos DB as a JSON object.

## Example: Adding a book

### Screen 1
Please enter the following information:
* __Title?__ James and the Giant Peach
* __Author?__ Ronald Dahl
* __Cover image?__ C:\users\downloads\james-peach-cover.png
* __Save? Enter 'Y' or 'N'.__ Y

### Backend happenings
The application then saves the image _james-peach-cover.png_ to Azure Blob Storage. In Azure Cosmos DB, it saves the
following JSON entry.

__NOTE:__ the "cover" entry is a relative path.

```json
{
    "title": "James and the Giant Peach",
    "author": {
        "lastname": "Dahl",
        "firstname": "Ronald",
    },
    "cover": "/images/covers/dahl/ronald/james-and-the-giant-peach.png"
}
```

# Find a book

The screen should prompt the user for the following inputs:
1. Search by book title?
1. Search by author?

Afterwards, it should query Azure Cosmos DB for any books that match the search criteria and list them. From there, if a
user selects a book, it will display the book's full entry. In addition, it will download the image to a TEMP file
location and display the path to the user.

## Example: Finding a book

### Screen 1
__How would you like to find the book?__
1. Search by book title?
1. Search by author?

_User enters '2'_

### Screen 2
* __What is the author's name?__ J.K. Rowling
* __Here are books by JK Rowling. Please enter the number you wish to view.__
    1. Harry Potter and the Order of the Phoenix
    1. Harry Potter and the Philosopher's Stone
    1. Harry Potter and the Prisoner of Azkaban
    1. ...

_User enters '3'_

### Screen 3
Here are the details of "Harry Potter and the Prisoner of Azkaban":
* Title: Harry Potter and the Prisoner of Azkaban
* Author: Rowling, J.K.
* Cover: "C:\TEMP\rowling\harry-potter-and-the-prisoner-of-azkaban.png"
    * This was downloaded and saved to the user's TEMP folder.

# Delete a book

The user can delete a book by its title.

## Example: Deleting a book
The screen prompts the user with the following:

### Screen 1
__Enter title of book to delete:__ Taken

### Screen 2
Here are matching books. Enter the number to delete:
1. Lee, Corbin - Taken
1. Smith, John - Taken
1. Sunny, Lee -Taken

_User enters '3'_

### Screen 3

__Delete "Sunny, Lee - Taken"? Enter Y or N.__

_User enters 'Y'_

Book is deleted.