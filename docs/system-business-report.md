# Báo cáo nghiệp vụ hệ thống Smart Parking

Ngày cập nhật: 02/07/2026  
Phạm vi: Backend Spring Boot hiện tại và các màn hình chính của frontend đang sử dụng API.

## 1. Tổng quan hệ thống

Smart Parking là hệ thống quản lý bãi giữ xe gồm 3 nhóm nghiệp vụ chính:

1. Người dùng đăng ký tài khoản, đăng ký xe, mua hoặc thanh toán gói gửi xe.
2. Nhân viên vận hành xe vào, xe ra, duyệt đăng ký xe, xử lý hỗ trợ và theo dõi phiên gửi xe.
3. Quản trị viên quản lý tài khoản, nhân sự, cấu hình hệ thống, thông báo, dữ liệu bãi và kiểm soát nghiệp vụ vận hành.

Backend là REST API Spring Boot, base path chính là `/api/v1`. Các API thanh toán hiện nằm thêm dưới `/api/payments/...` và một số API cũ dưới `/api/customer`, `/api/subscriptions`.

## 2. Vai trò trong hệ thống

### 2.1 Khách chưa đăng nhập

Khách chưa đăng nhập chỉ được truy cập các chức năng công khai:

- Xem trang đăng nhập, đăng ký.
- Đăng ký tài khoản mới.
- Đăng nhập bằng email/mật khẩu.
- Đăng nhập bằng Google.
- Xem một số dữ liệu công khai như loại xe, bảng gói phí, tổng quan khu vực bãi nếu API cho phép.
- Nhận callback thanh toán từ VNPay.

Khách chưa đăng nhập không được đăng ký xe, mua gói, xem thông tin cá nhân, thao tác vận hành bãi hoặc quản trị hệ thống.

### 2.2 USER / Driver

USER là người dùng gửi xe. Đây là role mặc định khi đăng ký tài khoản.

Chức năng chính:

- Đăng nhập, đăng xuất.
- Xem dashboard cá nhân.
- Đăng ký xe/thẻ xe của mình.
- Nhập biển số xe thủ công. Ảnh biển số chỉ là minh chứng tùy chọn, không phải nguồn dữ liệu bắt buộc.
- Tải ảnh minh chứng nếu có: CCCD, bằng lái, giấy đăng ký/cà vẹt xe, ảnh biển số.
- Theo dõi trạng thái hồ sơ đăng ký xe.
- Xem danh sách xe đã được duyệt/đăng ký.
- Chọn gói biểu phí cho xe đã đăng ký.
- Thanh toán khoản chờ thanh toán bằng cổng thanh toán đang được tích hợp nếu hệ thống tạo invoice.
- Xem thông báo cá nhân.
- Gửi yêu cầu hỗ trợ/sự cố.
- Xem và cập nhật hồ sơ cá nhân.

Quy tắc nghiệp vụ của USER:

- USER không có quyền tự duyệt đăng ký xe.
- USER chỉ xem được đăng ký xe, gói, hóa đơn và thông báo thuộc tài khoản của mình.
- Nếu xe chưa đăng ký gói, xe vẫn có thể vào bãi như khách vãng lai và tính phí theo lượt.
- Nếu xe có gói đang hoạt động, hệ thống xử lý như xe có vé tháng/gói hợp lệ.

### 2.3 STAFF

STAFF là nhân viên vận hành bãi. Role STAFF được xác định khi tài khoản có nhân viên đang hoạt động trong bảng nhân sự.

Chức năng chính:

- Truy cập workspace vận hành.
- Kiểm tra xe vào.
- Xác nhận xe vào và tạo phiên gửi xe.
- Kiểm tra xe ra.
- Xác nhận xe ra, tính phí nếu là khách vãng lai, giải phóng vị trí đỗ.
- Xem danh sách phiên gửi xe.
- Xem dashboard vận hành.
- Duyệt hoặc từ chối hồ sơ đăng ký xe.
- Tạo đăng ký xe cho tài khoản user.
- Khi tạo đăng ký xe cho user, có thể chọn:
  - Tạm chưa đăng ký gói.
  - Chọn một gói và đánh dấu đã thu tiền mặt.
  - Chọn một gói nhưng chưa thu tiền, để user tự thanh toán trong tài khoản.
- Xem danh sách người dùng theo quyền được cấp.
- Xử lý hỗ trợ/sự cố nếu được phân quyền.
- Xem và gửi thông báo theo chức năng được mở cho staff.

Quy tắc nghiệp vụ của STAFF:

- STAFF không phải chủ sở hữu tài khoản user, nhưng được thao tác nghiệp vụ thay user trong phạm vi vận hành.
- STAFF không nên có quyền thay đổi role nhân sự cấp cao nếu không được cấu hình.
- Khi thu tiền mặt cho gói xe, hệ thống phải ghi nhận invoice là đã thanh toán.
- Khi chưa thu tiền mặt, hệ thống tạo khoản chờ thanh toán để user tự thanh toán sau.

### 2.4 ADMIN

ADMIN là quản trị viên hệ thống.

Chức năng chính:

- Có toàn bộ quyền của STAFF.
- Quản lý tài khoản người dùng.
- Tạo tài khoản nhân viên/người dùng theo quyền API cho phép.
- Khóa/mở khóa tài khoản.
- Cập nhật role nhân sự.
- Quản lý danh sách nhân viên.
- Quản lý cấu hình hệ thống.
- Quản lý dữ liệu vị trí đỗ.
- Quản lý thông báo hệ thống.
- Xem nhật ký/audit log.
- Xem và xử lý sự cố/hỗ trợ.
- Kiểm soát danh sách đăng ký xe.
- Xóa đăng ký xe khi cần.

Quy tắc nghiệp vụ của ADMIN:

- ADMIN là role có quyền cao nhất trong hệ thống.
- Các thao tác thay đổi tài khoản, role, cấu hình, thông báo và audit phải được kiểm soát chặt.
- ADMIN có thể thao tác đăng ký xe/gói cho user như STAFF.

## 3. Cơ chế xác thực và phân quyền

### 3.1 Đăng ký tài khoản

Form đăng ký chuẩn chỉ gồm 4 trường:

- Họ và tên.
- Gmail/email.
- Mật khẩu.
- Xác nhận mật khẩu.

Hệ thống không sử dụng trường tên đăng nhập riêng. Email là định danh đăng nhập.

Luồng xử lý:

1. Người dùng nhập 4 thông tin bắt buộc.
2. Frontend kiểm tra mật khẩu và xác nhận mật khẩu khớp nhau.
3. Backend kiểm tra email đã tồn tại hay chưa.
4. Backend tạo `User` với role mặc định là `USER`.
5. Backend tạo hồ sơ `Customer` tương ứng.
6. Backend trả JWT token để người dùng đăng nhập vào hệ thống.

### 3.2 Đăng nhập

Người dùng đăng nhập bằng email và mật khẩu. Sau khi xác thực mật khẩu, backend phát JWT.

Role hiệu lực được xác định như sau:

- Nếu tài khoản là admin hợp lệ, role là `ADMIN`.
- Nếu tài khoản có hồ sơ employee đang hoạt động, role là `STAFF`.
- Nếu không phải employee/admin, role là `USER`.

### 3.3 Đăng nhập Google

Đăng nhập Google dùng email Google làm định danh. Nếu email chưa tồn tại, hệ thống tạo tài khoản user mới. Nút trên màn hình đăng nhập phải thể hiện đúng là "Đăng nhập bằng Google", không phải "Đăng ký bằng Google".

## 4. Ma trận quyền chức năng

| Chức năng | Guest | USER | STAFF | ADMIN |
|---|---:|---:|---:|---:|
| Đăng ký tài khoản | Có | Không cần | Không cần | Không cần |
| Đăng nhập/Google login | Có | Có | Có | Có |
| Quên mật khẩu | Có | Có | Có | Có |
| Xem hồ sơ cá nhân | Không | Có | Có | Có |
| Đăng ký xe của mình | Không | Có | Không | Không |
| Xem đăng ký xe của mình | Không | Có | Không | Không |
| Tạo đăng ký xe cho user | Không | Không | Có | Có |
| Duyệt/từ chối đăng ký xe | Không | Không | Có | Có |
| Xóa đăng ký xe | Không | Không | Có | Có |
| Chọn/mua gói cho xe | Không | Có | Có, thay user | Có, thay user |
| Đánh dấu đã thu tiền mặt | Không | Không | Có | Có |
| Thanh toán online | Không | Có | Không | Không |
| Kiểm tra xe vào | Không | Không | Có | Có |
| Xác nhận xe vào | Không | Không | Có | Có |
| Kiểm tra xe ra | Không | Không | Có | Có |
| Xác nhận xe ra | Không | Không | Có | Có |
| Quản lý vị trí đỗ | Không | Không | Có | Có |
| Quản lý tài khoản | Không | Không | Giới hạn | Có |
| Quản lý thông báo | Không | Không | Có/Giới hạn | Có |
| Cấu hình hệ thống | Không | Không | Không/Giới hạn | Có |
| Xem audit log | Không | Không | Có/Giới hạn | Có |
| Gửi hỗ trợ | Không | Có | Không | Không |
| Xử lý hỗ trợ | Không | Không | Có | Có |

## 5. Nghiệp vụ đăng ký xe

### 5.1 USER tự đăng ký xe

Mục tiêu: người dùng gửi hồ sơ để staff/admin duyệt xe.

Luồng:

1. USER vào màn "Đăng ký thẻ xe".
2. USER chọn loại xe: xe máy hoặc ô tô.
3. USER nhập biển số xe thủ công.
4. USER có thể tải ảnh minh chứng.
5. USER gửi yêu cầu xét duyệt.
6. Hồ sơ ở trạng thái chờ duyệt.
7. STAFF/ADMIN xem hồ sơ trong danh sách pending.
8. STAFF/ADMIN duyệt hoặc từ chối.
9. Nếu duyệt, xe được ghi nhận trong hệ thống.

Quy tắc:

- Biển số xe là dữ liệu do người dùng nhập tay.
- OCR/eKYC không được xem là nguồn bắt buộc cho biển số.
- Ảnh biển số xe là tùy chọn.
- Các ảnh CCCD/bằng lái/giấy đăng ký xe phục vụ đối chiếu, tùy quy định nghiệp vụ từng bãi.
- Không được bắt user phải upload ảnh biển số nếu đã nhập biển số thủ công.

### 5.2 STAFF/ADMIN tạo đăng ký xe cho user

Mục tiêu: nhân viên hoặc admin tạo xe trực tiếp cho tài khoản user.

Luồng:

1. STAFF/ADMIN vào màn "Đăng ký xe cho user".
2. Chọn user cần đăng ký.
3. Chọn loại xe.
4. Nhập biển số xe.
5. Tải ảnh minh chứng nếu có.
6. Chọn option gói:
   - Tạm chưa đăng ký gói.
   - Chọn gói tháng/quý/nửa năm/năm.
7. Nếu chọn gói, STAFF/ADMIN chọn trạng thái thanh toán:
   - Đã thu tiền mặt.
   - Chưa thu tiền, gửi về tài khoản user thanh toán.
8. Tạo đăng ký xe.
9. Xe phải xuất hiện trong tài khoản user sau khi user đăng nhập.

Quy tắc:

- Nếu "Tạm chưa đăng ký gói", xe được đăng ký nhưng không có subscription active. Khi vào bãi, xe tính như khách vãng lai.
- Nếu chọn gói và tích "Đã thu tiền mặt", hệ thống tạo subscription active và invoice thành công.
- Nếu chọn gói nhưng không tích "Đã thu tiền mặt", hệ thống tạo subscription/invoice chờ thanh toán để user tự thanh toán.
- Không cần màn đăng ký gói riêng trong back-office vì chức năng chọn gói đã nằm trong màn đăng ký xe cho user.
- Không bắt buộc nhập ảnh biển số xe.

## 6. Nghiệp vụ biểu phí và gói gửi xe

### 6.1 Gói phí

Gói phí có thể theo loại xe và thời hạn:

- Gói tháng.
- Gói quý.
- Gói nửa năm.
- Gói năm.

Mỗi gói có thông tin:

- Loại xe áp dụng.
- Thời hạn.
- Giá tiền.
- Trạng thái hoạt động.
- Quyền lợi hoặc mô tả gói.

### 6.2 USER chọn gói

Luồng:

1. USER vào màn "Biểu phí thẻ xe".
2. Hệ thống hiển thị danh sách xe đã đăng ký/được duyệt của user.
3. USER chọn xe.
4. USER chọn gói.
5. Hệ thống tạo subscription và invoice.
6. USER thực hiện thanh toán online.
7. Sau khi thanh toán thành công, subscription chuyển sang active.

Quy tắc:

- Màn biểu phí phải hiển thị xe mà user đã đăng ký.
- Nếu admin/staff đăng ký xe cho user thì xe đó vẫn phải xuất hiện tại màn biểu phí của user.
- Xe chưa có gói vẫn được vào bãi như khách vãng lai.

### 6.3 STAFF/ADMIN chọn gói khi đăng ký xe

Chức năng chọn gói được gộp vào màn đăng ký xe cho user.

Các trạng thái thanh toán:

- Tạm chưa đăng ký gói: không tạo gói, không tạo khoản thanh toán bắt buộc.
- Đã thu tiền mặt: tạo gói active ngay.
- Chưa thu tiền: tạo khoản chờ thanh toán cho user.

## 7. Nghiệp vụ thanh toán

### 7.1 Thanh toán VNPay

Luồng tổng quát:

1. Hệ thống tạo order thanh toán.
2. Người dùng được chuyển sang cổng VNPay.
3. VNPay trả kết quả qua return URL hoặc IPN.
4. Backend xác minh kết quả.
5. Nếu thành công, invoice/subscription được cập nhật.
6. Frontend hiển thị kết quả thanh toán.

API VNPay có các chức năng:

- Kiểm tra trạng thái order.
- Nhận IPN.
- Nhận return callback.
- Hủy order nếu còn phù hợp.

### 7.2 Thanh toán Stripe

Hệ thống có module Stripe order:

- Tạo PaymentIntent cho hóa đơn/giao dịch.
- Xác nhận trạng thái order.
- Nhận webhook từ Stripe.
- Cập nhật invoice/subscription hoặc phiên gửi xe khi thanh toán thành công.

### 7.3 Thanh toán tiền mặt

Tiền mặt dùng khi STAFF/ADMIN thu trực tiếp từ user tại bãi.

Quy tắc:

- Khi tích "Đã thu tiền mặt", hệ thống không bắt user thanh toán lại.
- Invoice phải thể hiện đã thanh toán.
- Subscription phải active theo gói đã chọn.
- Cần lưu dấu vết để đối soát sau này.

## 8. Nghiệp vụ xe vào bãi

Mục tiêu: kiểm tra xe, xác định quyền vào bãi và tạo phiên gửi xe.

Luồng:

1. STAFF/ADMIN nhập hoặc quét biển số xe.
2. Hệ thống kiểm tra xe đã đăng ký hay chưa.
3. Hệ thống kiểm tra có subscription active hay không.
4. Hệ thống kiểm tra tình trạng chỗ trống.
5. STAFF/ADMIN xác nhận xe vào.
6. Hệ thống tạo `ParkingOrder` hoặc phiên gửi xe.
7. Hệ thống cập nhật trạng thái slot thành đang sử dụng.

Quy tắc:

- Xe có gói active: xử lý theo vé tháng/gói đã mua.
- Xe không có gói: xử lý như khách vãng lai.
- Xe chưa đăng ký vẫn có thể được xử lý theo chính sách khách vãng lai nếu hệ thống cho phép.
- Không được tạo nhiều phiên đang mở cho cùng một xe nếu phiên trước chưa kết thúc.

## 9. Nghiệp vụ xe ra bãi

Mục tiêu: kết thúc phiên gửi xe, tính phí và giải phóng slot.

Luồng:

1. STAFF/ADMIN nhập hoặc quét biển số xe/order.
2. Hệ thống tìm phiên gửi xe đang mở.
3. Hệ thống tính thời gian gửi.
4. Nếu xe có gói active, phí có thể bằng 0 hoặc theo chính sách gói.
5. Nếu xe là khách vãng lai, hệ thống tính phí theo cấu hình.
6. STAFF/ADMIN xác nhận xe ra.
7. Hệ thống đóng phiên gửi xe.
8. Hệ thống cập nhật slot thành trống.

Quy tắc:

- Không xác nhận xe ra nếu không có phiên gửi xe đang mở.
- Sau khi xe ra, phiên không được chỉnh sửa tùy tiện.
- Slot phải được giải phóng khi phiên kết thúc.

## 10. Quản lý bãi và vị trí đỗ

Dữ liệu bãi gồm:

- Parking facility.
- Building.
- Floor.
- Zone.
- Slot.

Chức năng:

- Xem danh sách slot.
- Xem chi tiết slot.
- Tạo slot.
- Cập nhật slot.
- Xóa slot.
- Xem tổng quan khu vực bãi.
- Xem số chỗ trống, chỗ đang dùng, trạng thái theo tầng/khu.

Quy tắc:

- Slot có trạng thái như trống, đang sử dụng, bảo trì hoặc các trạng thái nghiệp vụ tương ứng.
- Khi xe vào, slot chuyển sang đang sử dụng.
- Khi xe ra, slot quay về trống nếu không có ràng buộc khác.

## 11. Quản lý tài khoản và nhân sự

### 11.1 Quản lý user

Chức năng:

- Xem danh sách user.
- Tìm kiếm theo tên, email, số điện thoại.
- Xem chi tiết user.
- Tạo user.
- Cập nhật user.
- Xóa hoặc khóa user tùy API.
- Gán/chỉnh trạng thái tài khoản.
- Xem tình trạng thanh toán.

### 11.2 Quản lý employee

Chức năng:

- Xem danh sách nhân viên.
- Tạo nhân viên/tài khoản nhân viên.
- Cập nhật role.
- Khóa/mở khóa nhân viên.

Quy tắc role:

- Role hiệu lực của STAFF/ADMIN liên quan đến hồ sơ employee đang hoạt động.
- User thường không có employee active thì là USER.

## 12. Quản lý thông báo

Chức năng admin/staff:

- Tạo thông báo.
- Xem danh sách thông báo.
- Xem chi tiết thông báo.
- Cập nhật thông báo.
- Gửi thông báo.
- Xóa thông báo.

Chức năng user:

- Xem thông báo của mình.
- Đánh dấu đã đọc từng thông báo.
- Đánh dấu đọc tất cả.

Hệ thống có thể dùng device token/Firebase để gửi push notification.

## 13. Hỗ trợ, sự cố và audit

### 13.1 Hỗ trợ khách hàng

USER:

- Gửi yêu cầu hỗ trợ.
- Xem yêu cầu hỗ trợ của mình.

STAFF/ADMIN:

- Xem danh sách sự cố/yêu cầu hỗ trợ.
- Phản hồi sự cố.
- Đóng sự cố.

### 13.2 Audit log

Audit log dùng để theo dõi các hành động quan trọng:

- Đăng nhập/quản lý tài khoản.
- Duyệt đăng ký xe.
- Thay đổi cấu hình.
- Xác nhận thanh toán.
- Thao tác xe vào/ra.

Audit đặc biệt quan trọng với nghiệp vụ thu tiền mặt.

## 14. Cấu hình hệ thống

ADMIN có thể quản lý cấu hình hệ thống, ví dụ:

- Thông tin bãi.
- Chính sách tính phí.
- Cấu hình vận hành.
- Cấu hình thông báo.
- Các tham số nghiệp vụ khác nếu API hỗ trợ.

Quy tắc:

- Chỉ ADMIN hoặc role được cấp quyền mới được sửa cấu hình.
- Thay đổi cấu hình nên có audit log.

## 15. Mô hình dữ liệu chính

Các entity quan trọng:

| Entity | Ý nghĩa |
|---|---|
| `User` | Tài khoản đăng nhập, thông tin cá nhân, email, password hash, trạng thái |
| `Customer` | Hồ sơ khách hàng gắn với user |
| `Employee` | Hồ sơ nhân viên/admin gắn với user |
| `Vehicle` | Xe đã được ghi nhận trong hệ thống |
| `VehicleType` | Loại xe: xe máy, ô tô |
| `VehicleRegistration` | Hồ sơ đăng ký xe chờ duyệt hoặc đã xử lý |
| `FeePackage` | Gói biểu phí |
| `FeePackagePriceHistory` | Lịch sử giá gói |
| `FeeSubscription` | Gói đã đăng ký cho xe/user |
| `FeeSubscriptionInvoice` | Hóa đơn/khoản thanh toán của gói |
| `ParkingOrder` | Phiên gửi xe/order gửi xe |
| `ParkingSlot` | Vị trí đỗ xe |
| `ParkingFacility` | Bãi xe |
| `Building` | Tòa/khu nhà |
| `ParkingFloor` | Tầng bãi |
| `ParkingZone` | Khu trong tầng/bãi |
| `VNPayOrder` | Order thanh toán VNPay |
| `Notification` | Thông báo |
| `DeviceToken` | Token thiết bị nhận push notification |
| `RefreshToken` | Refresh token |
| `Card` | Thẻ xe hoặc thẻ liên kết nghiệp vụ |

## 16. API chính theo module

### 16.1 Auth

Base: `/api/v1/auth`

- `POST /login`: đăng nhập.
- `POST /register`: đăng ký tài khoản.
- `POST /signup`: đăng ký tài khoản, alias của register nếu backend hỗ trợ.
- `POST /google-login`: đăng nhập Google.
- `POST /refresh-token`: cấp token mới.
- `POST /logout`: đăng xuất.

### 16.2 User và account

- `/api/v1/users`: quản lý user.
- `/api/v1/users/me`: xem tài khoản hiện tại.
- `/api/v1/admin/accounts/users`: quản lý user trong admin.
- `/api/v1/admin/accounts/employees`: quản lý nhân viên.

### 16.3 Đăng ký xe

Base: `/api/v1/vehicle-registrations`

- `POST /`: USER tạo đăng ký xe của mình.
- `GET /my`: USER xem đăng ký xe của mình.
- `POST /users/{userId}`: STAFF/ADMIN tạo đăng ký xe cho user.
- `GET /pending`: STAFF/ADMIN xem hồ sơ chờ duyệt.
- `GET /`: STAFF/ADMIN xem danh sách đăng ký.
- `GET /{id}`: xem chi tiết theo quyền.
- `PUT /{id}/review`: STAFF/ADMIN duyệt/từ chối.
- `DELETE /{id}`: STAFF/ADMIN xóa đăng ký.

### 16.4 Gói phí và subscription

- `GET /api/v1/fee-packages`: xem gói phí.
- `GET /api/v1/fee-subscriptions/my-vehicles`: USER xem xe có thể mua gói.
- `POST /api/v1/fee-subscriptions`: USER tạo đăng ký gói.
- `GET /api/v1/fee-subscriptions/my`: USER xem gói của mình.
- `POST /api/v1/fee-subscriptions/{id}/payment`: tạo thanh toán cho gói.
- `PATCH /api/v1/fee-subscriptions/{id}/cancel`: hủy gói.

### 16.5 Xe vào

Base: `/api/v1/parking-entry`

- `POST /check`: kiểm tra xe trước khi vào.
- `POST /confirm`: xác nhận xe vào.

### 16.6 Xe ra

Base: `/api/v1/parking-exit`

- `POST /check`: kiểm tra xe trước khi ra.
- `POST /{orderId}/confirm`: xác nhận xe ra.

### 16.7 Slot và tổng quan bãi

- `/api/v1/parking-slots`: CRUD vị trí đỗ.
- `/api/v1/parking-area-summary/options`: danh sách tùy chọn tổng quan.
- `/api/v1/parking-area-summary`: tổng quan bãi.

### 16.8 Thông báo

- `/api/v1/admin/notifications`: quản lý thông báo.
- `/api/v1/notifications`: xem thông báo/chung và đăng ký device token.
- `/api/customer/notifications`: thông báo của customer.

### 16.9 Hỗ trợ và vận hành

- `/api/customer/support`: USER gửi và xem hỗ trợ của mình.
- `/api/v1/incidents`: STAFF/ADMIN quản lý sự cố.
- `/api/v1/audit-logs`: xem audit log.
- `/api/v1/staff/operations-dashboard`: dashboard vận hành.
- `/api/v1/staff/parking-operations`: dữ liệu vận hành.
- `/api/v1/staff/parking-sessions`: phiên gửi xe.

### 16.10 Thanh toán

VNPay:

- `/api/payments/vnpay/orders/{txnRef}/status`
- `/api/payments/vnpay/ipn`
- `/api/payments/vnpay/return`
- `/api/payments/vnpay/orders/{txnRef}/cancel`

Dev VNPay mock:

- `/api/dev/vnpay/simulate-payment`
- `/api/dev/vnpay/orders`

## 17. Quy tắc nghiệp vụ quan trọng cần giữ đúng

1. Form đăng ký tài khoản không có trường username.
2. Email là định danh đăng nhập chính.
3. Nút Google ở màn login phải là "Đăng nhập bằng Google".
4. USER nhập biển số xe thủ công.
5. Ảnh biển số xe không bắt buộc nếu đã nhập biển số.
6. OCR/eKYC chỉ hỗ trợ đối chiếu, không được làm hỏng luồng nhập biển số.
7. Xe do admin/staff đăng ký cho user phải hiển thị trong tài khoản user.
8. Màn biểu phí phải lấy đúng xe đã đăng ký của user.
9. Back-office không cần màn đăng ký gói riêng nếu gói đã nằm trong đăng ký xe cho user.
10. Option "Tạm chưa đăng ký gói" nghĩa là xe vào bãi như khách vãng lai.
11. Chọn gói và tích "Đã thu tiền mặt" nghĩa là đã thanh toán, subscription active ngay.
12. Chọn gói nhưng không tích "Đã thu tiền mặt" nghĩa là tạo khoản chờ user thanh toán.
13. STAFF/ADMIN xử lý xe vào/ra, USER không tự xác nhận xe vào/ra.
14. Slot phải đổi trạng thái đúng theo phiên gửi xe.
15. Các API ngoài auth phải yêu cầu JWT trừ các endpoint public được cấu hình.
16. ADMIN có quyền quản trị cao nhất, STAFF chỉ thao tác vận hành và nghiệp vụ được cấp.

## 18. Ghi chú kỹ thuật hiện tại

- Backend dùng Spring Boot 3.3.5, Java 17.
- JWT được gửi qua header `Authorization: Bearer <token>`.
- Database là PostgreSQL/Supabase.
- `ddl-auto: none`, backend không tự sinh schema.
- Role user được xử lý động khi login dựa trên tài khoản employee active.
- Một số endpoint cũ còn tồn tại như `/api/subscriptions`; nếu không còn dùng ở frontend nên rà soát trước khi xóa để tránh ảnh hưởng dữ liệu/luồng cũ.
- VNPay mock chỉ nên dùng cho môi trường dev/test.

## 19. Đề xuất kiểm soát để tránh lỗi nghiệp vụ

1. Chuẩn hóa một API duy nhất cho đăng ký tài khoản: ưu tiên `/api/v1/auth/register`, hoặc giữ `/signup` làm alias nhưng phải test cả hai.
2. Tách rõ 2 trạng thái của xe:
   - Xe đã đăng ký/được duyệt.
   - Xe có gói active.
3. Không để frontend phụ thuộc vào OCR biển số.
4. Viết test cho các case:
   - User đăng ký tài khoản không có username.
   - Admin/staff tạo xe cho user, user login thấy xe.
   - Tạo xe không gói, vào bãi tính khách vãng lai.
   - Tạo xe có gói đã thu tiền mặt, subscription active.
   - Tạo xe có gói chưa thu tiền, user thấy khoản chờ thanh toán.
   - Vehicle entry/exit cập nhật slot đúng.
5. Rà soát endpoint cũ trước khi xóa code để tránh frontend hoặc dữ liệu lịch sử còn dùng.
