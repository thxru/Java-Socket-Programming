package tcp;

class GradeCalculator{

    double subA;
    double subB;
    double subC;
    GradeCalculator(double subA,double subB, double subC){

        this.subA = subA;
        this.subB = subB;
        this.subC = subC;
    }

    double getAverage(){
        return (subA+subB+subC)/3;
    }

    String getGrade(){

        double ave = getAverage();

        if (ave >=85)
            return "A+";
        if (ave>=75)
            return "A";
        if (ave>=70)
            return "A-";
        if (ave>=65)
            return "B+";
        if (ave>=60)
            return "B";
        if (ave>=55)
            return "B-";
        if (ave>=50)
            return "C+";
        if (ave>=40)
            return "C";
        else
            return "D";
    }
}