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
    println(name.substring(0,5))
    println(name.contains("Coding"))
    println(name.replace("Coding","Ranojit"))
    println(name.uppercase())
    println(name.isEmpty())
    println(name.isNullOrBlank())
    println(name)
    println(multiLine)
    println(escapedString)


    // StringBuilder , StringBuffer

    val sb = StringBuilder("Hello")
    sb.append(" World"," Hi Kotlin")
    sb.insert(0,"hi ")
    println(sb)

    val sbuffer = StringBuffer("Hello")
    sbuffer.append(" World"," Hi Kotlin")
    sbuffer.insert(0,"hi ")
    println(sbuffer)
}