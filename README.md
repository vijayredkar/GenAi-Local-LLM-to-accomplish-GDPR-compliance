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
7. cURLs for testing
    - curl --request GET \
  --url 'http://localhost:8888/gen-ai/v1/llm/retrieve?text=The%20amount%20of%20credit%20card%20payment%20for%20Mr.%20Gary%20Thompson%20with%205671425%20EIDA%2C%20SSN%20764132566%2C%20and%207536785621%20contact%20number%20is%20USD%20752.63'
    - curl --request GET \
  --url 'http://localhost:8888/gen-ai/v1/llm/retrieve?text=El%20monto%20del%20pago%20con%20tarjeta%20de%20cr%C3%A9dito%20para%20el%20Sr.%20Gary%20Thompson%20con%205671425%20EIDA%2C%20764132566%20SSN%20y%20n%C3%BAmero%20de%20contacto%207536785621%20es%20de%20USD%20752.63'
