package commonly.commonlybe.certificate.document;

/** 서식의 "총  개월  일" 칸. 년 단위 칸이 없어 개월 + 일로만 낸다. */
public record WorkPeriod(int months, int days) {
}
