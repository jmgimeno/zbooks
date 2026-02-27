-- Table names match the Magnum CamelToSnakeCase mapping of BookRow / ReadingRow
CREATE TABLE IF NOT EXISTS book_row (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  author      VARCHAR(255) NOT NULL,
  editor      VARCHAR(255) NOT NULL,
  year        INT          NOT NULL,
  evaluation  INT          CHECK (evaluation IS NULL OR (evaluation >= 1 AND evaluation <= 5))
);

CREATE TABLE IF NOT EXISTS reading_row (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  book_id     BIGINT   NOT NULL REFERENCES book_row(id) ON DELETE CASCADE,
  start_date  DATE     NOT NULL,
  end_date    DATE     NOT NULL,
  CONSTRAINT chk_reading_row_dates CHECK (end_date >= start_date)
);
