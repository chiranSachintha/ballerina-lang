type Person record {|
    string firstName;
    string lastName;
    int age;
|};

type FullName record{|
	string firstName;
	string lastName;
|};

function testLimitClauseWithQueryExprNegative() returns Person[] {

    Person p1 = {firstName: "Alex", lastName: "George", age: 23};
    Person p2 = {firstName: "Ranjan", lastName: "Fonseka", age: 30};
    Person p3 = {firstName: "John", lastName: "David", age: 33};

    Person[] personList = [p1, p2, p3];

    Person[] outputPersonList =
            from var person in personList
            limit checkAge(33)
            select {
                   firstName: person.firstName,
                   lastName: person.lastName,
                   age: person.age
            };

    return outputPersonList;
}

function testLimitClauseWithQueryExprNegativeLimit() returns Person[] {

    Person p1 = {firstName: "Alex", lastName: "George", age: 23};
    Person p2 = {firstName: "Ranjan", lastName: "Fonseka", age: 30};
    Person p3 = {firstName: "John", lastName: "David", age: 33};

    Person[] personList = [p1, p2, p3];

    Person[] outputPersonList =
            from var person in personList
            limit -1
            select {
                   firstName: person.firstName,
                   lastName: person.lastName,
                   age: person.age
            };

    return outputPersonList;
}

function testLimitClauseWithQueryActionNegative() returns FullName[] {

    Person p1 = {firstName: "Alex", lastName: "George", age: 23};
    Person p2 = {firstName: "Ranjan", lastName: "Fonseka", age: 30};
    Person p3 = {firstName: "John", lastName: "David", age: 33};

    Person[] personList = [p1, p2, p3];
    FullName[] nameList = [];

    var x =  from var person in personList
            limit checkAge(33)
            do {
                FullName fullName = {firstName: person.firstName, lastName: person.lastName};
                nameList[nameList.length()] = fullName;
            };

    return nameList;
}

function testLimitClauseWithQueryActionNegativeLimit() returns FullName[] {

    Person p1 = {firstName: "Alex", lastName: "George", age: 23};
    Person p2 = {firstName: "Ranjan", lastName: "Fonseka", age: 30};
    Person p3 = {firstName: "John", lastName: "David", age: 33};

    Person[] personList = [p1, p2, p3];
    FullName[] nameList = [];

    var x =  from var person in personList
            limit -1
            do {
                FullName fullName = {firstName: person.firstName, lastName: person.lastName};
                nameList[nameList.length()] = fullName;
            };

    return nameList;
}

function checkAge(int age) returns boolean {
    return age == 35;
}

function testLimitClauseAfterSelectClause() {
        Person p1 = {firstName: "Alex", lastName: "George", age: 23};
    Person p2 = {firstName: "Ranjan", lastName: "Fonseka", age: 30};
    Person p3 = {firstName: "John", lastName: "David", age: 33};

    Person[] personList = [p1, p2, p3];

    Person[] outputPersonList =
            from var person in personList
            select {
                   firstName: person.firstName,
                   lastName: person.lastName,
                   age: person.age
            }
            limit 2;
}
