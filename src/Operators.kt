// Arithmetic : +,/,-,*,%
// Comparison : <,>,<=,>=
// Logical : &&.||. !
fun main(){

    val A = 40
    val B = 30
    println(A+B)
    println(A-B)
    println(A/B)
    println(A*B)
    println(A<B)
    println(A>B)
    println(A%B)

    var num = 5
    println(num++)
    println(num)
    println(++num)

    var d = 20
    d+=5

    val var1 = false
    val var2 = true
    println("var1 && var 2 = ${var1 && var2}")
    println("var1 || var 2 = ${var1 || var2}")

}