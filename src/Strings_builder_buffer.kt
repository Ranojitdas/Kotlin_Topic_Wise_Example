// Strings are immutable , each time we assign a new value to the variable , a new object is created
// StringBuilder and StringBuffer are mutable , so it can be changed in place

fun main(){

    val name: String = "Coding"

    val multiLine = """
        Coding
        Sleep Eat 
        Coding
    """.trimIndent()

    val escapedString = "This is an \" escaped \" string"

    val country = "India"

    println("My name is $name and i lived in $country")
    println("My name length is ${name.length}")

    // Useful Methods
    println(name.length)
    println(name[0])
















    println(name)
    println(multiLine)
    println(escapedString)
}