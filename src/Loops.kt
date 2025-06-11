
fun main(){

    val fruits = arrayOf("banana","apple","mango")

    for (items in fruits){
        println(items)
    }

    for ((index,item) in fruits.withIndex()){
        if (index ==4) continue
        println("item at $index is $item")
    }

    var i = 10
    while (i>5){
        println(i)
        i--
    }

    i = 5
    do {
        println(i)
        i--
    }
    while (i<5 && i>0)

    i = 10
    while (true){
    println(i)
        if (i==2) break
        i--
    }
}

