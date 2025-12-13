# 🎓 HỆ THỐNG ĐĂNG KÝ HỌC PHẦN QUA MICROSERVICES & KAFKA
**(High-Performance Course Registration System)**

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![Kafka](https://img.shields.io/badge/Apache_Kafka-Event_Driven-black)
![React](https://img.shields.io/badge/Frontend-ReactJS-blue)

### 📖 Giới thiệu

Đây là đề tài **Niên luận ngành Mạng máy tính & Truyền thông dữ liệu** với chủ đề:
> **"Giải pháp giúp ổn định hệ thống đăng ký học phần trong giờ cao điểm dựa trên kiến trúc Microservices và Kafka Streams"**

Hệ thống giải quyết bài toán "nghẽn cổ chai" (bottleneck) tại các cổng đăng ký tín chỉ truyền thống. Bằng cách áp dụng kiến trúc **Microservices** kết hợp cơ chế xử lý bất đồng bộ qua **Kafka** và bộ nhớ đệm **RocksDB**, hệ thống có khả năng chịu tải hàng ngàn request đồng thời mà không gây sập Database hay treo hệ thống.

---

### 🚀 Giải pháp Kỹ thuật & Tính năng

#### 1. Kiến trúc Hệ thống (Microservices Event-Driven)
Hệ thống không ghi trực tiếp yêu cầu vào Database. Thay vào đó, nó sử dụng mô hình:
1.  **Peak Shaving (Cắt đỉnh tải):** Request đăng ký được đẩy vào **Kafka Topic** (Message Queue) để xếp hàng xử lý.
2.  **Async Processing (Xử lý bất đồng bộ):** Registration Service tiêu thụ message từ Kafka và xử lý tuần tự, đảm bảo tính toàn vẹn dữ liệu.
3.  **High Performance State Store:** Sử dụng **RocksDB** (nhúng trong Course Service) để quản lý số lượng Slot (Chỗ trống) với độ trễ cực thấp (microsecond), giảm tải tối đa cho MySQL.

#### 2. Tính năng Người dùng
* **🔐 Xác thực SSO (Keycloak):** Đăng nhập tập trung, bảo mật cao, phân quyền (Sinh viên/Admin).
* **📅 Lập kế hoạch học tập:** Sinh viên chọn trước các môn học dự định đăng ký.
* **⚡ Đăng ký Real-time:** Xử lý đăng ký nhanh chóng, phản hồi kết quả (Thành công/Hết chỗ) qua cơ chế polling/socket.
* **🛠️ Quản trị (Admin Dashboard):** Quản lý môn học, lớp học phần, cấu hình sĩ số, xem thống kê đăng ký.

---

### 🛠 Công nghệ sử dụng

| Phân lớp | Công nghệ | Chi tiết |
| :--- | :--- | :--- |
| **Backend** | Java 17, Spring Boot | Discovery (Eureka), Gateway, Config Server |
| **Database** | MySQL | Lưu trữ dữ liệu bền vững (Users, Courses) |
| **State Store**| **RocksDB** | Lưu trữ trạng thái Slot lớp học phần (High speed) |
| **Messaging** | **Apache Kafka** | Xử lý luồng sự kiện (Event Streaming) |
| **Identity** | Keycloak | Quản lý định danh và phân quyền (OAuth2/OIDC) |
| **Frontend** | ReactJS | Giao diện người dùng (TailwindCSS/AntDesign) |
| **DevOps** | Docker | Đóng gói môi trường (Containerization) |

---

### ⚙️ Cấu trúc Services

Hệ thống bao gồm các Service độc lập giao tiếp qua REST API và Kafka:

| Service Name | Port | Mô tả |
| :--- | :--- | :--- |
| `discovery-server` | `8761` | Eureka Server - Quản lý định danh dịch vụ |
| `api-gateway` | `8888` | Cổng vào duy nhất, định tuyến & xác thực Token |
| `identity-service` | `8081` | Kết nối Keycloak, quản lý User Info |
| `course-service` | `8082` | Quản lý Môn học, RocksDB Store |
| `registration-service`| `8083` | **Core:** Nhận request -> Kafka -> Xử lý đăng ký |
| `studyplan-service` | `8084` | Quản lý kế hoạch học tập cá nhân |
| `frontend` | `3000` | Web Client (ReactJS) |

---

### 💻 Hướng dẫn Cài đặt & Triển khai

#### 1. Yêu cầu môi trường
* Java JDK 17+
* Node.js (v16 trở lên)
* Docker & Docker Compose (Bắt buộc)
* Maven

#### 2. Khởi tạo Infrastructure (Docker)
Tại thư mục gốc của dự án, chạy lệnh sau để khởi tạo MySQL, Kafka, Keycloak:

```bash
docker-compose up -d

Đợi khoảng 1-2 phút để các container khởi động hoàn toàn.

3. Cấu hình Database & Keycloak
MySQL:

Tạo database rỗng tên: microservices_dkhp (hoặc để Hibernate tự tạo).

Keycloak:

Truy cập: http://localhost:8181

Tài khoản Admin mặc định: admin / admin (hoặc xem trong file docker-compose).

Quan trọng: Vào mục Import, chọn file realm-export.json (nằm trong thư mục source code) để nạp sẵn cấu hình Realm, Client, Role và Users mẫu.

4. Chạy Backend Services
Khởi động các Service theo đúng thứ tự sau để tránh lỗi:

🔴 Discovery Server (DiscoveryServerApplication) -> Chờ chạy xong.

🟡 API Gateway (ApiGatewayApplication).

🟢 Các Service còn lại (Chạy song song hoặc tuần tự):

IdentityServiceApplication

CourseServiceApplication (Lưu ý: Service này sẽ tạo folder data cho RocksDB)

RegistrationServiceApplication

StudyPlanServiceApplication

5. Chạy Frontend
Mở terminal tại thư mục frontend (hoặc client):

Bash

npm install   # Cài đặt thư viện
npm start     # Chạy ứng dụng tại http://localhost:3000
📝 Hướng dẫn Sử dụng & Test
1. Đăng nhập (SSO)
Truy cập http://localhost:3000, nhấn Đăng nhập.

Admin: User admin / Pass (xem trong keycloak)

Sinh viên: User student / Pass (xem trong keycloak)

2. Quy trình Đăng ký
Đăng nhập tài khoản Sinh viên.

Vào menu "Kế hoạch học tập", thêm các môn muốn học.

Chuyển sang "Đăng ký học phần", chọn lớp và bấm Đăng ký.

Hệ thống hiển thị trạng thái "Đang xử lý...", sau đó trả về kết quả.

3. Kiểm thử chịu tải (Load Test với JMeter)
Để chứng minh khả năng "cắt đỉnh tải" của Kafka:

Mở Apache JMeter, import file TestPlan.jmx (nếu có).

Cấu hình Thread Group: 1000 users, Ramp-up 5s.

API Endpoint: POST http://localhost:8888/api/registration.

Kết quả mong đợi:

Tỉ lệ lỗi (Error Rate): 0%.

Database không bị quá tải, request được Kafka xếp hàng và xử lý dần.

👨‍💻 Thông tin Tác giả
Sinh viên thực hiện: Trương Văn Quy

MSSV: B2204965

Lớp: Mạng máy tính & Truyền thông dữ liệu K48

Học phần: CT439 - Niên luận ngành

GVHD: TS. Ngô Bá Hùng

Trường: Đại học Cần Thơ (CTU)

Dự án phục vụ mục đích học tập và nghiên cứu.