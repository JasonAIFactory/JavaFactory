# Java 코딩 연습 (백지부터)

면접 실전처럼 **빈 메서드만 있고 본인이 채워야** 합니다. 각 파일에 main()이 있어 바로 실행해서 테스트 가능.

---

## 🛠️ VS Code 셋업 (5분)

### 1. JDK 설치 (mac)

```bash
brew install openjdk@17

# 환경변수 설정 (zsh)
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# 확인
java -version
```

### 2. VS Code 익스텐션

마켓플레이스에서 **"Extension Pack for Java"** (Microsoft) 검색 → 설치.
이거 하나로 6개 익스텐션 자동 설치 (Language Support, Debugger, Test Runner, Maven, Gradle, Project Manager).

### 3. 실행

- `.java` 파일 열기
- `main()` 메서드 위에 뜨는 **"Run"** 클릭 (또는 F5)
- 터미널에 결과 출력됨

---

## 📝 연습 파일 5개

| 파일 | 난이도 | 주제 |
| --- | --- | --- |
| `Problem01_PrefixCount.java` | 🟢 쉬움 | Codility Task 2 (실제 출제) |
| `Problem02_GroupByKey.java` | 🟢 쉬움 | HashMap + 집계 |
| `Problem03_ValidateOrders.java` | 🟢 쉬움 | 입력 검증 |
| `Problem04_FindDuplicates.java` | 🟡 중간 | Set 활용 |
| `Problem05_SimpleCache.java` | 🟡 중간 | LinkedHashMap LRU |

---

## 🎯 사용법

1. 파일 열기
2. **TODO** 주석 부분 본인이 채우기
3. F5로 실행
4. 모든 테스트가 `✓ PASS`면 성공
5. 영어로 풀이 설명 한 번 말해보기 (학습 자료의 🗣️ 스크립트 참고)

---

## 📚 추천 진행 순서

1. **빈 파일에서 시작** — 정답 보지 말고 직접 풀기
2. **20분 안에 안 풀리면** — 학습 자료 (`03-coding-practice.md`) 보고 힌트만 얻기
3. **풀이 후** — 영어로 설명 1번 말해보기
4. **다음 날 다시** — 같은 문제 다시 풀어보기 (반복이 중요)
