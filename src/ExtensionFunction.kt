
fun String.greet():String{
    return "hello $this"
}

fun Int.isEvenorOdd(): Boolean{
    return this%2 == 0

}
fun main(){
    println("Kotlin".greet())
    print("Enter the number : ")
    val a = readln().toInt()
    var value = a.isEvenorOdd()
    if(value == true){
        println("$a is Even")
    }
    else println("$a is Odd")


}