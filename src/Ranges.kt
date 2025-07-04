
fun main(){
    val range1 = 1..10   // .. is the range operator

    for (item in range1){
        println(item)
    }

    for (item in 1..5 step 2){   // here step specifies the increment order
        println(item)
    }
    for (item in 1 until 5 step 2){   // until a little bit different from .. it doesn't include 15
        println(item)
    }



    for(item in 15 downTo 1 step 3){ // downTo use for reverse range
        println(item)
    }

    val charRange = 'a'..'z'
    for (item in charRange){  //for printing character we can use either item or char
        println(item)
    }

}