1. GenAi Local LLM Ollama with Chroma Vector DB to solve data privacy concerns.
2. Problem statement & solution approach - https://vijayredkar.medium.com/banknext-case-study-ai-llm-to-solve-gdpr-challenge-a6e86c99d29f
3. GenAi Local LLM Architecture -
   ![BankNext_GenAi_LLM_VectorDB_Arch ](https://github.com/vijayredkar/GenAi-Local-LLM-to-accomplish-GDPR-compliance/assets/25388646/d97d7573-9bc7-49b7-97a3-3394690bd049)
5. Local application setup
    - Launch Chroma DB -
    docker run -p 8000:8000 chromadb/chroma
6. Launch Application with local Ollama LLM server
   - git clone https://github.com/vijayredkar/GenAi-Local-LLM-to-accomplish-GDPR-compliance.git
   - cd <YOUR-PATH>\GenAi-Local-LLM-to-accomplish-GDPR-compliance\gen-ai-llm-gdpr
   - mvn clean install
   - java -jar target/gen-ai-llm-local-data-privacy.jar
