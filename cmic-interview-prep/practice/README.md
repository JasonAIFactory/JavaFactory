# Java 코딩 연습 (백지부터)

빈 메서드만 있고 본인이 채우는 형식. 각 파일에 `main()`이 있어 VS Code에서 바로 F5 실행 가능.

---

## 🛠️ VS Code 셋업 (5분)

### 1. JDK 설치 (mac)

```bash
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version
```

### 2. VS Code 익스텐션

마켓플레이스에서 **"Extension Pack for Java"** (Microsoft) 검색 → 설치.

### 3. 실행

- `.java` 파일 열기
- `main()` 메서드 위에 뜨는 **"Run"** 클릭 (또는 F5)
- 터미널에 결과 출력됨

---

## 📚 추천 진행 순서

### 1단계: Basic 10개 (기초 다지기)

각 파일은 5~15분이면 끝남. **언어 자체에 익숙해지는 게 목적**.

| 파일 | 주제 |
| --- | --- |
| `Basic01_PrintAndVariables.java` | 변수 선언 + println |
| `Basic02_ArraysAndLoop.java` | 배열 + for 루프 |
| `Basic03_IfElse.java` | if / else if / else |
| `Basic04_Method.java` | 메서드 정의 + 호출 |
| `Basic05_Strings.java` | 문자열 메서드 |
| `Basic06_ArrayList.java` | List add/get/iterate |
| `Basic07_HashMap.java` | Map put/get/contains |
| `Basic08_SimpleClass.java` | 클래스 + 생성자 + toString |
| `Basic09_Inheritance.java` | 상속 + 다형성 |
| `Basic10_TryCatch.java` | 예외 처리 |

→ 정답은 `BASIC_SOLUTIONS.md`

### 2단계: Problem 5개 (실전 패턴)

기초가 익숙해지면 도전. CMiC가 cost management ERP라서 이런 패턴이 매일 나옴.

| 파일 | 난이도 | 주제 |
| --- | --- | --- |
| `Problem01_PrefixCount.java` | 🟢 쉬움 | Codility Task 2 (실제 출제) |
| `Problem02_GroupByKey.java` | 🟢 쉬움 | HashMap + 집계 |
| `Problem03_ValidateOrders.java` | 🟢 쉬움 | 입력 검증 |
| `Problem04_FindDuplicates.java` | 🟡 중간 | Set 활용 |
| `Problem05_SimpleCache.java` | 🟡 중간 | LinkedHashMap LRU |

→ 정답은 `SOLUTIONS.md`

---

## 🎯 사용법

1. 파일 열기
2. **TODO** 주석 부분 본인이 채우기
3. F5로 실행
4. 출력이 expected와 일치하면 성공
5. 영어로 풀이 한 번 말해보기 (학습 자료의 🗣️ 참고)

⚠️ 정답 보기 전에 **20분 안에 풀리지 않으면 학습 자료 (`02-java-basics.md`) 보고 힌트만 얻기**. 그래도 안 되면 정답 보기.

---

## 📂 정답 파일 (각 문제마다 매칭)

각 문제에 대해 정답이 별도 Java 파일로 있음 (실행해서 비교 가능):

- `Basic01_PrintAndVariables.java` ↔ `Basic01_PrintAndVariables_Solution.java`
- `Basic02_ArraysAndLoop.java` ↔ `Basic02_ArraysAndLoop_Solution.java`
- ... (Basic01~10 모두)
- `Problem01_PrefixCount.java` ↔ `Problem01_PrefixCount_Solution.java`
- ... (Problem01~05 모두)
- `OOP01_Override.java` ↔ `OOP01_Override_Solution.java`
- ... (OOP01~05 모두)

추가로 `BASIC_SOLUTIONS.md` / `SOLUTIONS.md` / `OOP_SOLUTIONS.md`에 영어 설명 스크립트 포함.
